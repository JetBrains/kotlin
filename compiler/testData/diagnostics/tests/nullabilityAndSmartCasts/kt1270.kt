// !DIAGNOSTICS: -UNUSED_VARIABLE
//KT-1270 Poor highlighting when trying to dereference a nullable reference

package kt1270

fun foo() {
    val sc = java.util.HashMap<String, SomeClass>()[""]
    val value = sc<!UNSAFE_CALL!>.<!>value
}

private class SomeClass() {
    val value : Int = 5
}
