// WITH_STDLIB
// SKIP_MANGLE_VERIFICATION

// MODULE: lib
// FILE: l.kt

interface I {
    fun foo(): String
}

enum class E(val i: I, val l: () -> String) {
    A(object : I {
        override fun foo(): String = "AI"
    }, { "AL" }) ,
    B(object : I {
        override fun foo(): String = "BI"
    }, { "BL" }),
    C(object : I {
        override fun foo(): String = "CI"
    }, { "CL" })
}

// MODULE: main(lib)
// FILE: m.kt


fun box(): String {
    var result = ""

    result += E.A.i.foo()
    result += E.A.l()

    result += E.B.i.foo()
    result += E.B.l()

    result += E.C.i.foo()
    result += E.C.l()

    if (result != "AIALBIBLCICL") return "FAIL: $result"

    return "OK"
}