// LANGUAGE: +FunctionalTypeWithExtensionAsSupertype
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^ IMPLEMENTING_FUNCTION_INTERFACE: Implementing function interface is prohibited in JavaScript

fun foo(): String {
    class Local : Int.() -> String {
        override fun invoke(p1: Int): String {
            return "O"
        }
    }
    val x : Int.() -> String = {"K"}
    return Local()(1) + x(1)
}

fun box(): String = foo()