// !LANGUAGE: +InlineClasses

class C {
    inline class IC1(val s: String)

    companion object {
        inline class IC2(val s: String)
    }
}

object O {
    inline class IC3(val s: String)
}

interface I {
    inline class IC4(val s: String)
}

fun box(): String {
    if (C.IC1("OK").s != "OK") return "FAIL 1"
    if (C.Companion.IC2("OK").s != "OK") return "FAIL 2"
    if (O.IC3("OK").s != "OK") return "FAIL 3"
    if (I.IC4("OK").s != "OK") return "FAIL 4"
    return "OK"
}