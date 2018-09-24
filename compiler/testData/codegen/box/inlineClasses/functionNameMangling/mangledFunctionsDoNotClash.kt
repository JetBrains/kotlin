// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class Id(val id: String)

inline class Name(val name: String)

inline class Password(val password: String)

fun test(id: Id) {
    if (id.id != "OK") throw AssertionError()
}

fun test(name: Name) {
    if (name.name != "OK") throw AssertionError()
}

fun test(password: Password) {
    if (password.password != "OK") throw AssertionError()
}

fun box(): String {
    test(Id("OK"))
    test(Name("OK"))
    test(Password("OK"))

    return "OK"
}