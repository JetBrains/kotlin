// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class Id(val id: String)

fun test(id: Id, str: String) {
    if (id.id != "OK" && str != "1") throw AssertionError()
}

fun test(str: String, id: Id) {
    if (id.id != "OK" && str != "2") throw AssertionError()
}

fun box(): String {
    test(Id("OK"), "1")
    test("2", Id("OK"))

    return "OK"
}