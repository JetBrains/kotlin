package kotlin_.cinterop

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

    private val allocatedChunks = mutableListOf<NativePointed>()

    override fun alloc(size: Long, align: Int): NativePointed {
        val res = nativeHeap.alloc(size, align)
        try {
            allocatedChunks.add(res)
            return res
        } catch (e: Throwable) {
            nativeHeap.free(res)
            throw e
        }
    }

    fun clear() {
        allocatedChunks.forEach {
            nativeHeap.free(it)
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
        alloc(CVariable.sizeOf<T>(), CVariable.alignOf<T>()).reinterpret()

/**
 * Allocates C array of given elements type and length.
 *
 * @param T must not be abstract
 */
inline fun <reified T : CVariable> NativePlacement.allocArray(length: Long): CArray<T> =
        alloc(CVariable.sizeOf<T>() * length, CVariable.alignOf<T>()).reinterpret()

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
                                                              initializer: T.(Long)->Unit): CArray<T> {
    val res = allocArray<T>(length)

    (0 until length).forEach {
        res[it].initializer(it)
    }

    return res
}

/**
 * Allocates C array of given elements type and length, and initializes its elements applying given block.
 *
 * @param T must not be abstract
 */
inline fun <reified T : CVariable> NativePlacement.allocArray(length: Int, initializer: T.(Long)->Unit) =
        allocArray(length.toLong(), initializer)


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
        allocArrayOfPointersTo(elements.toList())

/**
 * Allocates C array of given values.
 */
inline fun <reified T : CPointer<*>>
        NativePlacement.allocArrayOf(vararg elements: T?): CArray<CPointerVarWithValueMappedTo<T>> {

    return allocArrayOf(elements.toList())
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
    elements.forEachIndexed { i, byte ->
        res[i].value = byte
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

        val bytes = ByteArray(this.length())
        bytes.forEachIndexed { i, byte ->
            bytes[i] = array[i].value
        }
        return String(bytes) // TODO: encoding
    }

    fun asCharPtr() = reinterpret<CInt8Var>()
}

fun CString.Companion.fromString(str: String?, placement: NativePlacement): CString? {
    if (str == null) {
        return null
    }

    val bytes = str.toByteArray() // TODO: encoding
    val len = bytes.size
    val nativeBytes = nativeHeap.allocArray<CInt8Var>(len + 1)

    bytes.forEachIndexed { i, byte ->
        nativeBytes[i].value = byte
    }
    nativeBytes[len].value = 0

    return CString.fromArray(nativeBytes)
}

fun CPointer<CInt8Var>.asCString() = CString.fromArray(this.reinterpret<CArray<CInt8Var>>().pointed)
fun String.toCString(placement: NativePlacement) = CString.fromString(this, placement)

class MemScope private constructor(private val arena: Arena) : NativePlacement by arena {
    val memScope: NativePlacement
        get() = this

    companion object {
        internal inline fun <R> use(block: MemScope.()->R): R {
            val memScope = MemScope(Arena())
            try {
                return memScope.block()
            } finally {
                memScope.arena.clear()
            }
        }
    }
}

/**
 * Runs given [block] providing allocation of memory
 * which will be automatically disposed at the end of this scope.
 */
inline fun <R> memScoped(block: MemScope.()->R): R {
    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE") // TODO: it is a hack
    return MemScope.use(block)
}