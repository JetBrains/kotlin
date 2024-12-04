// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Id<T: String>(val id: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Name<T: String>(val name: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Password<T: String>(val password: T)

fun test(id: Id<String>) {
    if (id.id != "OK") throw AssertionError()
}

fun test(name: Name<String>) {
    if (name.name != "OK") throw AssertionError()
}

fun test(password: Password<String>) {
    if (password.password != "OK") throw AssertionError()
}

fun box(): String {
    test(Id("OK"))
    test(Name("OK"))
    test(Password("OK"))

    return "OK"
}