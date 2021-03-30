// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY
// NI_EXPECTED_FILE

class C {
    typealias Self = C
    class Nested {
        class N2
        typealias Root = C
    }
    companion object X {
        val ok = "OK"
        class InCompanion
    }
}

val c = C.<!UNRESOLVED_REFERENCE!>Self<!>.Self()
val n = C.<!UNRESOLVED_REFERENCE!>Self<!>.Nested()
val x = C.<!UNRESOLVED_REFERENCE!>Self<!>.X
val n2 = C.Nested.<!UNRESOLVED_REFERENCE!>Root<!>.Nested.N2()
val ic = C.<!UNRESOLVED_REFERENCE!>Self<!>.InCompanion()
val ok = C.<!UNRESOLVED_REFERENCE!>Self<!>.ok
