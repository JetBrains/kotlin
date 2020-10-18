// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

public open class Test() {
    open public fun test() : Unit {
        System.out?.println(hello)
    }
    companion object {
        private val hello : String? = "Hello"
    }
}

fun box() : String {
    Test().test()
    return "OK"
}
