// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

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

val c = C.Self.<!UNRESOLVED_REFERENCE!>Self<!>()
val n = C.Self.<!UNRESOLVED_REFERENCE!>Nested<!>()
val x = C.Self.<!UNRESOLVED_REFERENCE!>X<!>
val n2 = C.Nested.Root.<!UNRESOLVED_REFERENCE!>Nested<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>N2<!>()
val ic = C.Self.<!UNRESOLVED_REFERENCE!>InCompanion<!>()
val ok = C.Self.ok
