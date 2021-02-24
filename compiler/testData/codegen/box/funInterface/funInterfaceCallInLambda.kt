// DONT_TARGET_EXACT_BACKEND: WASM

fun interface Action {
    fun run()
}

fun runAction(a: Action) {
    a.run()
}

fun builder(c: () -> Unit) {
    c()
}

fun box(): String {
    var res = "FAIL"
    builder {
        runAction {
            res = "OK"
        }
    }
    return res
}