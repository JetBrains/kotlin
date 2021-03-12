// !WITH_NEW_INFERENCE
package bar

fun main() {
    class Some

    Some[<!SYNTAX!><!>] <!UNRESOLVED_REFERENCE!>names<!> <!SYNTAX!>=<!> <!NO_GET_METHOD!>["ads"]<!>
}
