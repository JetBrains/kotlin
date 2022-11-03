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

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC2(val x: IC) {
    companion object {
        operator fun box(x: IC) = createInlineClassInstance<IC2>(x)
    }
}

fun foo(a: IC?) = a?.x ?: 0
fun bar(a: IC2?) = a?.x?.x ?: 0

fun box(): String {
    if (foo(IC(1)) != 42) return "Fail 1"
    if (bar(IC2(IC(1))) != 1) return "Fail 2"
    return "OK1"
}