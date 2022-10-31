// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +CustomEqualsInInlineClasses
// TARGET_BACKEND: JVM_IR

class A private constructor(x: Int) {
    companion object {
        fun foo() {
            A(42)
        }
    }
}

fun box(): String {
    A.foo()
    return "OK1"
}