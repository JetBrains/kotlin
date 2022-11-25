// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +CustomEqualsInInlineClasses
// TARGET_BACKEND: JVM_IR

@JvmInline
value class IC(val x: Int)

fun foo(vararg x: IC) = x[0].x

fun box(): String {
    return "OK"
}