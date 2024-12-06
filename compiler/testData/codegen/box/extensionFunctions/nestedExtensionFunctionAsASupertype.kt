// LANGUAGE: +FunctionalTypeWithExtensionAsSupertype
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^ IMPLEMENTING_FUNCTION_INTERFACE: Implementing function interface is prohibited in JavaScript
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

class A(val a: String)

class B :  A.(A.() -> String) -> String {
    override fun invoke(p1: A, p2: A.() -> String): String {
        return p1.a + p2(A("K"))
    }
}

fun box(): String {
    return B()(A("O")) { this.a }
}