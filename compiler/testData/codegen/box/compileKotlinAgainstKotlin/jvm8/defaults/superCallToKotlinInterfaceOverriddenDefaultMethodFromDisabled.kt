// LANGUAGE: +AllowSuperCallToJavaInterface
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND_MULTI_MODULE: JVM_MULTI_MODULE_IR_AGAINST_OLD, JVM_MULTI_MODULE_OLD_AGAINST_IR
// MODULE: lib
// JVM_DEFAULT_MODE: all
// FILE: I.kt
interface I {
    fun f(): Int = 1

    var p: Int
        get() = storage
        set(value) { storage = value }
}

private var storage = 11

// FILE: J.kt
interface J : I {
    override fun f(): Int = 2

    override var p: Int
        get() = storage
        set(value) { storage = value }
}

private var storage = 22

// MODULE: main(lib)
// JVM_DEFAULT_MODE: disable
// FILE: box.kt
interface K : I, J {
    fun i_f(): Int = super<I>.f()
    fun j_f(): Int = super<J>.f()

    var i_p: Int
        get() = super<I>.p
        set(value) { super<I>.p = value }
    var j_p: Int
        get() = super<I>.p
        set(value) { super<I>.p = value }
}

class C : K

fun box(): String {
    val c = C()

    if (c.i_f() != 1) return "Fail C.i_f"
    if (c.j_f() != 2) return "Fail C.j_f"
    if (c.f() != 2) return "Fail C.f"

    c.i_p = 14
    if (c.i_p != 14) return "Fail C.i_p"
    c.j_p = 15
    if (c.j_p != 15) return "Fail C.j_p"
    c.p = 16
    if (c.p != 16) return "Fail C.p"

    return "OK"
}
