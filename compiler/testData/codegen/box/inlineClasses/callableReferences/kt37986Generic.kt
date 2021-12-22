// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class R<T: Any>(val x: T)

fun useR(r: R<String>) {
    if (r.x as String != "OK") throw AssertionError("$r")
}

fun useR0(fn: () -> R<String>) {
    useR(fn())
}

fun useR1(r: R<String>, fn: (R<String>) -> R<String>) {
    useR(fn(r))
}

fun fnWithDefaultR(r: R<String> = R("OK")) = r

fun box(): String {
    useR0(::fnWithDefaultR)
    useR1(R("OK"), ::fnWithDefaultR)

    return "OK"
}