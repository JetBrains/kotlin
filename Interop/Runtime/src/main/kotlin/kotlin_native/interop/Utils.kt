package kotlin_native.interop

fun <T : NativeRef> malloc(type: NativeRef.TypeWithSize<T>) = type.byPtr(bridge.malloc(type.size))

fun free(ptr: NativePtr?) {
    if (ptr != null) {
        bridge.free(ptr)
    }
}

fun free(ref: NativeRef?) = free(ref.getNativePtr())

fun <T : NativeRef> Placement.allocNativeArrayOf(elemType: NativeRef.Type<T>, elements: List<T?>): NativeArray<RefBox<T>> {
    val res = this.alloc(array[elements.size](elemType.ref))
    elements.forEachIndexed { i, element ->
        res[i].value = element
    }
    return res
}

fun <T : NativeRef> Placement.allocNativeArrayOf(elemType: NativeRef.Type<T>, vararg elements: T?) =
        allocNativeArrayOf(elemType, elements.toList())

fun <T : NativeRef> mallocNativeArrayOf(elemType: NativeRef.Type<T>, vararg elements: T?) = heap.allocNativeArrayOf(elemType, *elements)

fun Placement.allocNativeArrayOf(elements: ByteArray): NativeArray<Int8Box> {
    val res = this.alloc(array[elements.size](Int8Box))
    elements.forEachIndexed { i, element ->
        res[i].value = element
    }
    return res
}

fun CString.Companion.fromString(str: String?): CString? {
    if (str == null) {
        return null
    }

    val bytes = str.toByteArray() // TODO: encoding
    val len = bytes.size
    val nativeBytes = malloc(NativeArray of Int8Box length (len + 1))

    bytes.forEachIndexed { i, byte ->
        nativeBytes[i].value = byte
    }
    nativeBytes[len].value = 0

    return CString.fromArray(nativeBytes)
}

fun NativeArray<Int8Box>.asCString() = CString.fromArray(this)
fun Int8Box.asCString() = CString.fromArray(NativeArray.byRefToFirstElem(this, Int8Box))
fun String.toCString() = CString.fromString(this)

class MemScope private constructor(private val arena: Arena) : Placement by arena {
    val memScope: Placement
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