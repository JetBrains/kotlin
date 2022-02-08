// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Id(val id: String)

fun test(id: Id) {
    if (id.id != "OK") throw AssertionError()
}

fun test(id: Id?) {
    if (id != null) throw AssertionError()
}

fun box(): String {
    test(Id("OK"))
    test(null)

    return "OK"
}