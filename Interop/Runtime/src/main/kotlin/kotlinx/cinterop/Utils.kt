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

fun NativePlacement.alloc(size: Int, align: Int) = alloc(size.toLong(), align)

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
    val res = allocArray<CInt8Var>(elements.size)
    var index = 0
    for (byte in elements) {
        res[index++].value = byte
    }
    return res
}

fun <T : CPointed> NativePlacement.allocPointerTo() = alloc<CPointerVar<T>>()

/**
 * The zero-terminated string.
 */
class CString private constructor(override val rawPtr: NativePtr) : CPointed {

    companion object {
        fun fromArray(array: CArray<CInt8Var>) = CString(array.rawPtr)
    }

    fun length(): Int {
        val array = reinterpret<CArray<CInt8Var>>()

        var res = 0
        while (array[res].value != 0.toByte()) {
            ++res
        }
        return res
    }

    override fun toString(): String {
        val array = reinterpret<CArray<CInt8Var>>()

        val len = this.length()
        val bytes = ByteArray(len)

        nativeMemUtils.getByteArray(array[0], bytes, len)
        return decodeFromUtf8(bytes) // TODO: encoding
    }

    fun asCharPtr() = reinterpret<CInt8Var>().ptr
}

fun CString.Companion.fromString(str: String?, placement: NativePlacement): CString? {
    if (str == null) {
        return null
    }

    val bytes = encodeToUtf8(str) // TODO: encoding
    val len = bytes.size
    val nativeBytes = placement.allocArray<CInt8Var>(len + 1)

    nativeMemUtils.putByteArray(bytes, nativeBytes[0], len)
    nativeBytes[len].value = 0

    return CString.fromArray(nativeBytes)
}

fun CPointer<CInt8Var>.asCString() = CString.fromArray(this.reinterpret<CArray<CInt8Var>>().pointed)
fun String.toCString(placement: NativePlacement) = CString.fromString(this, placement)

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
