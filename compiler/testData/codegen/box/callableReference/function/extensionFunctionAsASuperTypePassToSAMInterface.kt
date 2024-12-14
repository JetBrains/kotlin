// LANGUAGE: +FunctionalTypeWithExtensionAsSupertype
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^ IMPLEMENTING_FUNCTION_INTERFACE: Implementing function interface is prohibited in JavaScript
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.
var result = ""

fun interface SamInterface {
    fun Int.accept(): String
}

class A: Int.() -> String {
    override fun invoke(p1: Int): String {
        return "O"
    }
}

class B: (Int) -> String {
    override fun invoke(p1: Int): String {
        return "K"
    }
}

val a = SamInterface ((::A)())
val b = SamInterface ((::B)())

fun box(): String {
    with(a) {
        result += 1.accept()
    }
    with(b) {
        result += 2.accept()
    }
    return result
}