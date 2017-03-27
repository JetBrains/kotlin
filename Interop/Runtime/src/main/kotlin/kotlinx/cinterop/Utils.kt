package kotlinx.cinterop

interface NativePlacement {

    fun alloc(size: Long, align: Int): NativePointed

    fun alloc(size: Int, align: Int) = alloc(size.toLong(), align)
}

interface NativeFreeablePlacement : NativePlacement {
    fun free(mem: NativePointed)
}

object nativeHeap : NativeFreeablePlacement {
    override fun alloc(size: Long, align: Int) = nativeMemUtils.alloc(size, align)

    override fun free(mem: NativePointed) = nativeMemUtils.free(mem)
}

// TODO: implement optimally
class Arena(private val parent: NativeFreeablePlacement = nativeHeap) : NativePlacement {

    private val allocatedChunks = ArrayList<NativePointed>()

    override fun alloc(size: Long, align: Int): NativePointed {
        val res = parent.alloc(size, align)
        try {
            allocatedChunks.add(res)
            return res
        } catch (e: Throwable) {
            parent.free(res)
            throw e
        }
    }

    fun clear() {
        allocatedChunks.forEach {
            parent.free(it)
        }

        allocatedChunks.clear()
    }

}

/**
 * Allocates variable of given type.
 *
 * @param T must not be abstract
 */
inline fun <reified T : CVariable> NativePlacement.alloc(): T =
        alloc(sizeOf<T>(), alignOf<T>()).reinterpret()

/**
 * Allocates C array of given elements type and length.
 *
 * @param T must not be abstract
 */
inline fun <reified T : CVariable> NativePlacement.allocArray(length: Long): CArray<T> =
        alloc(sizeOf<T>() * length, alignOf<T>()).reinterpret()

/**
 * Allocates C array of given elements type and length.
 *
 * @param T must not be abstract
 */
inline fun <reified T : CVariable> NativePlacement.allocArray(length: Int): CArray<T> =
        allocArray(length.toLong())

/**
 * Allocates C array of given elements type and length, and initializes its elements applying given block.
 *
 * @param T must not be abstract
 */
inline fun <reified T : CVariable> NativePlacement.allocArray(length: Long,
                                                              initializer: T.(index: Long)->Unit): CArray<T> {
    val res = allocArray<T>(length)

    (0 .. length - 1).forEach { index ->
        res[index].initializer(index)
    }

    return res
}

/**
 * Allocates C array of given elements type and length, and initializes its elements applying given block.
 *
 * @param T must not be abstract
 */
inline fun <reified T : CVariable> NativePlacement.allocArray(length: Int,
                                                              initializer: T.(index: Int)->Unit): CArray<T> =
        allocArray(length.toLong()) { index ->
            this.initializer(index.toInt())
        }


/**
 * Allocates C array of pointers to given elements.
 */
fun <T : CPointed> NativePlacement.allocArrayOfPointersTo(elements: List<T?>): CArray<CPointerVar<T>> {
    val res = allocArray<CPointerVar<T>>(elements.size)
    elements.forEachIndexed { index, value ->
        res[index].value = value?.ptr
    }
    return res
}

/**
 * Allocates C array of pointers to given elements.
 */
fun <T : CPointed> NativePlacement.allocArrayOfPointersTo(vararg elements: T?) =
        allocArrayOfPointersTo(listOf(*elements))

/**
 * Allocates C array of given values.
 */
inline fun <reified T : CPointer<*>>
        NativePlacement.allocArrayOf(vararg elements: T?): CArray<CPointerVarWithValueMappedTo<T>> {

    return allocArrayOf(listOf(*elements))
}

/**
 * Allocates C array of given values.
 */
inline fun <reified T : CPointer<*>>
        NativePlacement.allocArrayOf(elements: List<T?>): CArray<CPointerVarWithValueMappedTo<T>> {

    val res = allocArray<CPointerVarWithValueMappedTo<T>>(elements.size)
    elements.forEachIndexed { index, value ->
        res[index].value = value
    }

    return res
}

fun NativePlacement.allocArrayOf(elements: ByteArray): CArray<CInt8Var> {
    val result = allocArray<CInt8Var>(elements.size)
    nativeMemUtils.putByteArray(elements, result, elements.size)
    return result
}

fun NativePlacement.allocArrayOf(vararg elements: Float): CArray<CFloat32Var> {
    val res = allocArray<CFloat32Var>(elements.size)
    var index = 0
    for (element in elements) {
        res[index++].value = element
    }
    return res
}

fun <T : CPointed> NativePlacement.allocPointerTo() = alloc<CPointerVar<T>>()

fun <T : CVariable> zeroValue(size: Int, align: Int): CValue<T> = object : CValue<T>() {
    override fun getPointer(placement: NativePlacement): CPointer<T> {
        val result = placement.alloc(size, align)
        nativeMemUtils.zeroMemory(result, size)
        return interpretCPointer(result.rawPtr)!!
    }

    override val size get() = size
}

inline fun <reified T : CVariable> zeroValue(): CValue<T> =
        zeroValue<T>(sizeOf<T>().toInt(), alignOf<T>())

private fun <T : CPointed> NativePlacement.placeBytes(bytes: ByteArray, align: Int): CPointer<T> {
    val result = this.alloc(size = bytes.size, align = align)
    nativeMemUtils.putByteArray(bytes, result, bytes.size)
    return interpretCPointer(result.rawPtr)!!
}

fun <T : CVariable> CPointed.readValues(size: Int, align: Int): CValues<T> {
    val bytes = ByteArray(size)
    nativeMemUtils.getByteArray(this, bytes, size)

    return object : CValues<T>() {
        override fun getPointer(placement: NativePlacement): CPointer<T> = placement.placeBytes(bytes, align)
        override val size get() = bytes.size
    }
}

inline fun <reified T : CVariable> T.readValues(count: Int): CValues<T> =
        this.readValues<T>(size = count * sizeOf<T>().toInt(), align = alignOf<T>())

fun <T : CVariable> CPointed.readValue(size: Int, align: Int): CValue<T> {
    val bytes = ByteArray(size)
    nativeMemUtils.getByteArray(this, bytes, size)
    return object : CValue<T>() {
        override fun getPointer(placement: NativePlacement): CPointer<T> = placement.placeBytes(bytes, align)
        override val size get() = bytes.size
    }
}

// Note: can't be declared as property due to possible clash with a struct field.
// TODO: find better name.
inline fun <reified T : CStructVar> T.readValue(): CValue<T> = this.readValue(sizeOf<T>().toInt(), alignOf<T>())

// TODO: optimize
fun <T : CVariable> CValues<T>.getBytes(): ByteArray = memScoped {
    val result = ByteArray(size)

    nativeMemUtils.getByteArray(
            source = this@getBytes.placeTo(memScope).reinterpret<CInt8Var>().pointed,
            dest = result,
            length = result.size
    )

    result
}

/**
 * Calls the [block] with temporary copy if this value as receiver.
 */
inline fun <reified T : CStructVar, R> CValue<T>.useContents(block: T.() -> R): R = memScoped {
    this@useContents.placeTo(memScope).pointed.block()
}

inline fun <reified T : CStructVar> CValue<T>.copy(modify: T.() -> Unit): CValue<T> = useContents {
    this.modify()
    this.readValue()
}

inline fun <reified T : CStructVar> cValue(initialize: T.() -> Unit): CValue<T> =
    zeroValue<T>().copy(modify = initialize)

inline fun <reified T : CVariable> createValues(count: Int, initializer: T.(index: Int) -> Unit) = memScoped {
    val array = allocArray<T>(count, initializer)
    array[0].readValues(count)
}

fun cValuesOf(vararg elements: Byte): CValues<CInt8Var> = object : CValues<CInt8Var>() {
    override fun getPointer(placement: NativePlacement) = placement.allocArrayOf(elements)[0].ptr
    override val size get() = 1 * elements.size
}

// TODO: optimize other [cValuesOf] methods:

fun cValuesOf(vararg elements: Short): CValues<CInt16Var> =
        createValues(elements.size) { index -> this.value = elements[index] }

fun cValuesOf(vararg elements: Int): CValues<CInt32Var> =
        createValues(elements.size) { index -> this.value = elements[index] }

fun cValuesOf(vararg elements: Long): CValues<CInt64Var> =
        createValues(elements.size) { index -> this.value = elements[index] }

fun cValuesOf(vararg elements: Float): CValues<CFloat32Var> = object : CValues<CFloat32Var>() {
    override fun getPointer(placement: NativePlacement) = placement.allocArrayOf(*elements)[0].ptr
    override val size get() = 4 * elements.size
}

fun cValuesOf(vararg elements: Double): CValues<CFloat64Var> =
        createValues(elements.size) { index -> this.value = elements[index] }

fun <T : CPointed> cValuesOf(vararg elements: CPointer<T>?): CValues<CPointerVar<T>> =
        createValues(elements.size) { index -> this.value = elements[index] }

fun ByteArray.toCValues() = cValuesOf(*this)
fun ShortArray.toCValues() = cValuesOf(*this)
fun IntArray.toCValues() = cValuesOf(*this)
fun LongArray.toCValues() = cValuesOf(*this)
fun FloatArray.toCValues() = cValuesOf(*this)
fun DoubleArray.toCValues() = cValuesOf(*this)
fun <T : CPointed> Array<CPointer<T>?>.toCValues() = cValuesOf(*this)

fun <T : CPointed> List<CPointer<T>?>.toCValues() = this.toTypedArray().toCValues()

/**
 * TODO: should the name of the function reflect the encoding?
 *
 * @return the value of zero-terminated UTF-8-encoded C string constructed from given [kotlin.String].
 */
val String.cstr: CValues<CInt8Var>
    get() {
        val bytes = encodeToUtf8(this)

        return object : CValues<CInt8Var>() {
            override val size get() = bytes.size + 1

            override fun getPointer(placement: NativePlacement): CPointer<CInt8Var> {
                val result = placement.allocArray<CInt8Var>(bytes.size + 1)
                nativeMemUtils.putByteArray(bytes, result, bytes.size)
                result[bytes.size].value = 0.toByte()
                return result[0].ptr
            }
        }
    }

/**
 * TODO: should the name of the function reflect the encoding?
 *
 * @return the [kotlin.String] decoded from given zero-terminated UTF-8-encoded C string.
 */
fun CPointer<CInt8Var>.toKString(): String {
    val nativeBytes = this.reinterpret<CArray<CInt8Var>>().pointed

    var length = 0
    while (nativeBytes[length].value != 0.toByte()) {
        ++length
    }

    val bytes = ByteArray(length)
    nativeMemUtils.getByteArray(nativeBytes, bytes, length)
    return decodeFromUtf8(bytes)
}

class MemScope : NativePlacement {

    private val arena = Arena()

    override fun alloc(size: Long, align: Int) = arena.alloc(size, align)

    fun clear() = arena.clear()

    val memScope: NativePlacement
        get() = this
}

/**
 * Runs given [block] providing allocation of memory
 * which will be automatically disposed at the end of this scope.
 */
inline fun <R> memScoped(block: MemScope.()->R): R {
    val memScope = MemScope()
    try {
        return memScope.block()
    } finally {
        memScope.clear()
    }
}
