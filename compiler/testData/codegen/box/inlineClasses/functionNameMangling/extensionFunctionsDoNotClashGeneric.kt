// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Id<T: String>(val id: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Name<T: String>(val name: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class Password<T: String>(val password: T)

fun Id<String>.test() {
    if (id != "OK") throw AssertionError()
}

fun Id<String>?.test() {
    if (this != null) throw AssertionError()
}

fun Name<String>.test() {
    if (name != "OK") throw AssertionError()
}

fun test(password: Password<String>) {
    if (password.password != "OK") throw AssertionError()
}

class Outer {
    fun Id<String>.testExn() {
        if (id != "OK") throw AssertionError()
    }

    fun Name<String>.testExn() {
        if (name != "OK") throw AssertionError()
    }

    fun testExn(password: Password<String>) {
        if (password.password != "OK") throw AssertionError()
    }

    fun testExns() {
        Id("OK").testExn()
        Name("OK").testExn()
        testExn(Password("OK"))
    }
}

fun box(): String {
    Id("OK").test()
    null.test()
    Name("OK").test()
    test(Password("OK"))

    Outer().testExns()

    return "OK"
}