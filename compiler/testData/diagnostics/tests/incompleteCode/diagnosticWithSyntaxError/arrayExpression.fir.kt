// !WITH_NEW_INFERENCE
package bar

fun main() {
    class Some

    Some[<!SYNTAX!><!>] <!UNRESOLVED_REFERENCE!>names<!> <!UNRESOLVED_REFERENCE!><!SYNTAX!>=<!> ["ads"]<!>
}