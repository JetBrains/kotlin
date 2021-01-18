// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: NATIVE

open class MyLogic {
    protected open val postfix = "ZZZ"
    open fun String.foo(prefix: String = "XXX"): String = transform(prefix + this + postfix)
    protected fun transform(a: String) = "$a:$a"
    fun result(): String {
        return "YYY".foo()
    }
}
open class MyLogicWithDifferentPostfix : MyLogic() {
    override val postfix = "WWW"
}

class MyLogicSpecified : MyLogic() {
    override fun String.foo(prefix: String): String = "$prefix::$this::$postfix"
}

fun box(): String {
    val result1 = MyLogic().result()
    if (result1 != "XXXYYYZZZ:XXXYYYZZZ") {
        return "fail1: ${result1}"
    }

    val result2 = MyLogicWithDifferentPostfix().result()
    if (result2 != "XXXYYYWWW:XXXYYYWWW") {
        return "fail2: ${result2}"
    }

    val result3 = MyLogicSpecified().result()
    if (result3 != "XXX::YYY::ZZZ") {
        return "fail3: ${result3}"
    }

    return  "OK"
}