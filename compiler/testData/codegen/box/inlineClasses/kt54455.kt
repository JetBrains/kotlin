// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC1(val value: Int)

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC2(val value: Int)

fun foo(x: IC1, y: IC2) = (x as Any) == y

fun box(): String {
    if ((IC1(1) as Any) == IC2(1)) return "Fail 1"
    if (foo(IC1(1), IC2(1))) return "Fail 2"
    return "OK"
}