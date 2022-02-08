// !DIAGNOSTICS: -UNUSED_VARIABLE
// Issue: KT-35075

fun foo() {}

fun main() {
    val x1 = <!UNRESOLVED_REFERENCE!>logger<!>::info?::<!UNRESOLVED_REFERENCE!>print<!>
    val x2 = <!UNRESOLVED_REFERENCE!>logger<!>?::info?::<!UNRESOLVED_REFERENCE!>print<!>
    val x3 = <!UNRESOLVED_REFERENCE!>logger<!>?::info::<!UNRESOLVED_REFERENCE!>print<!>
    val x4 = <!UNRESOLVED_REFERENCE!>logger<!>?::info?::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>
    val x5 = <!UNRESOLVED_REFERENCE!>logger<!>::info?::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>
    val x6 = <!UNRESOLVED_REFERENCE!>logger<!>!!::<!UNRESOLVED_REFERENCE!>info<!>?::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>
    val x7 = <!UNRESOLVED_REFERENCE!>logger<!>::info<!NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!>::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>
    val x8 = <!UNRESOLVED_REFERENCE!>logger<!>?::info<!NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!>::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>
    val x9 = <!UNRESOLVED_REFERENCE!>logger<!>!!::<!UNRESOLVED_REFERENCE!>info<!>?::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>
    val x10 = <!UNRESOLVED_REFERENCE!>logger<!>::info?::<!UNRESOLVED_REFERENCE!>print<!><!NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!>::<!UNRESOLVED_REFERENCE!>print<!>
    val x11 = <!UNRESOLVED_REFERENCE!>logger<!>!!::<!UNRESOLVED_REFERENCE!>info<!><!NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!>::<!UNRESOLVED_REFERENCE!>print<!><!NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!>::<!UNRESOLVED_REFERENCE!>print<!>
    val x12 = <!UNRESOLVED_REFERENCE!>logger<!>?::info<!NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!>::<!UNRESOLVED_REFERENCE!>print<!><!NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!>::<!UNRESOLVED_REFERENCE!>print<!>
    val x13 = 42?::<!UNRESOLVED_REFERENCE!>unresolved<!>?::<!UNRESOLVED_REFERENCE!>print<!>

    val x14 = <!UNRESOLVED_REFERENCE!>logger<!><!SYNTAX!>?!!::info?::print?::print<!>
    val x15 = <!UNRESOLVED_REFERENCE!>logger<!>::info<!SYNTAX!>?!!::print?::print<!>
    val x16 = <!UNRESOLVED_REFERENCE!>logger<!>!!?::<!UNRESOLVED_REFERENCE!>info<!>?::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>
    val x17 = <!UNRESOLVED_REFERENCE!>logger<!>::info<!NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!>?::<!UNRESOLVED_REFERENCE!>print<!>?::<!UNRESOLVED_REFERENCE!>print<!>

    // It must be OK
    val x18 = String?::hashCode <!USELESS_ELVIS!>?: ::foo<!>
    val x19 = String::hashCode <!USELESS_ELVIS!>?: ::foo<!>
    val x20 = String?::hashCode::hashCode
    val x21 = kotlin.String?::hashCode::hashCode
}
