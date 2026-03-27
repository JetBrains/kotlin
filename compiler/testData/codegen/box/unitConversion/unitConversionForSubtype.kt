// LANGUAGE: +UnitConversionsOnArbitraryExpressions
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ISSUE: KT-84393

abstract class SubString : () -> String {
    override fun invoke(): String {
        sideEffect += "OK"
        return "ignored"
    }
}

fun foo(f: () -> Unit) {
    f()
}

var sideEffect = ""

fun box(): String {
    val s = object : SubString() {}
    foo(s)
    return sideEffect
}
