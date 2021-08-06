// DONT_TARGET_EXACT_BACKEND: WASM

fun doCall(block: Any  ):Int {
    (block as () -> Unit)()
    return 1
}

fun test1()   =
    doCall {
        try {}
        finally {
            {}
        }
    }

fun box(): String {
    test1()
    return "OK"
}
