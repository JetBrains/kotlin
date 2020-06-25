// !DIAGNOSTICS: -UNUSED_VARIABLE
// Issue: KT-35075

fun foo() {}

fun main() {
    val x1 = <!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!>logger<!>::info<!>?::print<!>
    val x2 = <!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!>logger<!>?::info<!>?::print<!>
    val x3 = <!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!>logger<!>?::info<!>::print<!>
    val x4 = <!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!>logger<!>?::info<!>?::print<!>?::print<!>?::print<!>?::print<!>?::print<!>?::print<!>?::print<!>?::print<!>?::print<!>?::print<!>
    val x5 = <!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!>logger<!>::info<!>?::print<!>?::print<!>?::print<!>?::print<!>?::print<!>?::print<!>?::print<!>?::print<!>?::print<!>?::print<!>
    val x6 = <!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!>logger<!>!!::info<!>?::print<!>?::print<!>
    val x7 = <!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!>logger<!>::info!!::print<!>?::print<!>
    val x8 = <!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!>logger<!>?::info!!::print<!>?::print<!>
    val x9 = <!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!>logger<!>!!::info<!>?::print<!>?::print<!>
    val x10 = <!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!>logger<!>::info<!>?::print!!::print<!>
    val x11 = <!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!>logger<!>!!::info!!::print!!::print<!>
    val x12 = <!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!>logger<!>?::info!!::print!!::print<!>
    val x13 = <!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!>42?::unresolved<!>?::print<!>

    val x14 = <!UNRESOLVED_REFERENCE!>logger<!><!SYNTAX!>?!!::info?::print?::print<!>
    val x15 = <!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!>logger<!>::info<!><!SYNTAX!>?!!::print?::print<!>
    val x16 = <!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!>logger<!>!!?::info<!>?::print<!>?::print<!>
    val x17 = <!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!>logger<!>::info!!?::print<!>?::print<!>

    // It must be OK
    val x18 = String?::hashCode ?: ::foo
    val x19 = String::hashCode ?: ::foo
    val x20 = String?::hashCode::hashCode
    val x21 = kotlin.String?::hashCode::hashCode
}
