// LANGUAGE: +FunctionalTypeWithExtensionAsSupertype
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

interface I: (String) -> String

class C: String.() -> String, I {
    override fun invoke(p1: String): String = p1
}

fun box(): String {
    val c = C()
    if (c("OK") != "OK") return c("OK")
    val ext: String.() -> String = c
    return "OK".ext()
}