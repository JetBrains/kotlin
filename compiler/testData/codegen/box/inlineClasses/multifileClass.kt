// WITH_RUNTIME
// DONT_TARGET_EXACT_BACKEND: WASM

fun box(): String {
    val uia = uintArrayOf()
    val uia2 = uintArrayOf()
    // UIntArray is a multifile class, so we need to know where to search for extension method copyInto.
    uia.copyInto(uia2)
    return "OK"
}
