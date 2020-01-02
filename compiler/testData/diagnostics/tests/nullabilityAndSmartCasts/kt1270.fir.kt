// !DIAGNOSTICS: -UNUSED_VARIABLE
//KT-1270 Poor highlighting when trying to dereference a nullable reference

package kt1270

fun foo() {
    val sc = <!UNRESOLVED_REFERENCE!>java.util.<!UNRESOLVED_REFERENCE!>HashMap<!><String, SomeClass>()[""]<!>
    val value = sc.<!UNRESOLVED_REFERENCE!>value<!>
}

private class SomeClass() {
    val value : Int = 5
}
