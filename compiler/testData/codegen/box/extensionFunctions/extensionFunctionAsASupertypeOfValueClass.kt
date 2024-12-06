// LANGUAGE: +FunctionalTypeWithExtensionAsSupertype
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^ IMPLEMENTING_FUNCTION_INTERFACE: Implementing function interface is prohibited in JavaScript
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

OPTIONAL_JVM_INLINE_ANNOTATION
value class ValueClass(private val s: Int) : Int.() -> String {
    override fun invoke(p1: Int): String {
        return "OK"
    }
}

fun box(): String {
    return ValueClass(1)(1)
}
