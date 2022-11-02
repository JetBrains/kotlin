// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +CustomEqualsInInlineClasses
// TARGET_BACKEND: JVM_IR

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC(val x: Int) {
    companion object {
        operator fun box(x: Int) = createInlineClassInstance<IC>(42)
    }
}

fun foo(a: IC?) = a?.x ?: 0

fun box(): String {
    if (foo(IC(1)) != 42) return "Fail 1"
    return "OK"
}