package kotlin_native.interop

fun <T : NativeRef> malloc(type: NativeRef.TypeWithSize<T>) = type.byPtr(bridge.malloc(type.size))

fun <T : NativeRef, R> malloc(type: NativeRef.TypeWithSize<T>, action: (T) -> R): R {
    val ref = malloc(type)
    try {
        return action(ref)
    } finally {
        bridge.free(ref.ptr)
    }
}

fun free(ptr: NativePtr?) {
    if (ptr != null) {
        bridge.free(ptr)
    }
}

fun free(ref: NativeRef?) = free(ref.getNativePtr())

fun <T : NativeRef> mallocNativeArrayOf(elemType: NativeRef.Type<T>, vararg elements: T?): NativeArray<RefBox<T>> {
    val res = malloc(array[elements.size](elemType.ref))
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