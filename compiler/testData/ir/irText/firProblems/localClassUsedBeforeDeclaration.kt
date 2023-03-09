// FIR_IDENTICAL

// NO_SIGNATURE_DUMP
// ^KT-57430

fun box(): String {
    return object {
        val a = A("OK")
        inner class A(val ok: String)
    }.a.ok
}
