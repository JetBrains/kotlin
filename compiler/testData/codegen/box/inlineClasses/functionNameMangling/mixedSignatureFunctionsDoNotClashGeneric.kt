// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Id<T: String>(val id: T)

fun test(id: Id<String>, str: String) {
    if (id.id != "OK" && str != "1") throw AssertionError()
}

fun test(str: String, id: Id<String>) {
    if (id.id != "OK" && str != "2") throw AssertionError()
}

fun box(): String {
    test(Id("OK"), "1")
    test("2", Id("OK"))

    return "OK"
}