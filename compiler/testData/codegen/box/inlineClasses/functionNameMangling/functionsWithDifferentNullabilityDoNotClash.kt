// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class Id(val id: String)

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