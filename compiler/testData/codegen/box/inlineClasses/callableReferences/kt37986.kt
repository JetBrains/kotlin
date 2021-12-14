// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class R(val x: Any)

fun useR(r: R) {
    if (r.x as String != "OK") throw AssertionError("$r")
}

fun useR0(fn: () -> R) {
    useR(fn())
}

fun useR1(r: R, fn: (R) -> R) {
    useR(fn(r))
}

fun fnWithDefaultR(r: R = R("OK")) = r

fun box(): String {
    useR0(::fnWithDefaultR)
    useR1(R("OK"), ::fnWithDefaultR)

    return "OK"
}