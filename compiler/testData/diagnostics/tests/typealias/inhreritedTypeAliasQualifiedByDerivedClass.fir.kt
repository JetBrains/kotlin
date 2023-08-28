// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

open class Base {
    typealias Nested = String
}

class Derived : Base()

fun test(x: Derived.<!UNRESOLVED_REFERENCE!>Nested<!>) = x

fun Base.testWithImplicitReceiver(x: <!UNRESOLVED_REFERENCE!>Nested<!>) {
    val y: <!UNRESOLVED_REFERENCE!>Nested<!> = x
}
