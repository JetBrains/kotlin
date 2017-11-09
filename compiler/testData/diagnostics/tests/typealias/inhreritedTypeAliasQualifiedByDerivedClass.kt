// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

open class Base {
    typealias Nested = String
}

class Derived : Base()

fun test(x: Derived.<!UNRESOLVED_REFERENCE!>Nested<!>) = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!>

fun Base.testWithImplicitReceiver(x: <!UNRESOLVED_REFERENCE!>Nested<!>) {
    val y: <!UNRESOLVED_REFERENCE!>Nested<!> = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!>
}