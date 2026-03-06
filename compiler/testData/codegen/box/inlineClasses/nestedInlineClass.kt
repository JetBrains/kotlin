// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

class C {

    OPTIONAL_JVM_INLINE_ANNOTATION
    value class IC1(val s: String)

    companion object {

        OPTIONAL_JVM_INLINE_ANNOTATION
        value class IC2(val s: String)
    }
}

object O {

    OPTIONAL_JVM_INLINE_ANNOTATION
    value class IC3(val s: String)
}

interface I {

    OPTIONAL_JVM_INLINE_ANNOTATION
    value class IC4(val s: String)
}

fun box(): String {
    if (C.IC1("OK").s != "OK") return "FAIL 1"
    if (C.Companion.IC2("OK").s != "OK") return "FAIL 2"
    if (O.IC3("OK").s != "OK") return "FAIL 3"
    if (I.IC4("OK").s != "OK") return "FAIL 4"
    return "OK"
}