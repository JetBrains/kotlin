// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

class C {

    OPTIONAL_JVM_INLINE_ANNOTATION
    value class IC1<T: String>(val s: T)

    companion object {

        OPTIONAL_JVM_INLINE_ANNOTATION
        value class IC2<T: String>(val s: T)
    }
}

object O {

    OPTIONAL_JVM_INLINE_ANNOTATION
    value class IC3<T: String>(val s: T)
}

interface I {

    OPTIONAL_JVM_INLINE_ANNOTATION
    value class IC4<T: String>(val s: T)
}

fun box(): String {
    if (C.IC1("OK").s != "OK") return "FAIL 1"
    if (C.Companion.IC2("OK").s != "OK") return "FAIL 2"
    if (O.IC3("OK").s != "OK") return "FAIL 3"
    if (I.IC4("OK").s != "OK") return "FAIL 4"
    return "OK"
}