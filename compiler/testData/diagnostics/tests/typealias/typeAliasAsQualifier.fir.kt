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

val c = C.<!UNRESOLVED_REFERENCE!>Self<!>.<!UNRESOLVED_REFERENCE!>Self<!>()
val n = C.<!UNRESOLVED_REFERENCE!>Self<!>.<!UNRESOLVED_REFERENCE!>Nested<!>()
val x = C.<!UNRESOLVED_REFERENCE!>Self<!>.<!UNRESOLVED_REFERENCE!>X<!>
val n2 = C.Nested.<!UNRESOLVED_REFERENCE!>Root<!>.<!UNRESOLVED_REFERENCE!>Nested<!>.<!UNRESOLVED_REFERENCE!>N2<!>()
val ic = C.<!UNRESOLVED_REFERENCE!>Self<!>.<!UNRESOLVED_REFERENCE!>InCompanion<!>()
val ok = C.<!UNRESOLVED_REFERENCE!>Self<!>.<!UNRESOLVED_REFERENCE!>ok<!>
