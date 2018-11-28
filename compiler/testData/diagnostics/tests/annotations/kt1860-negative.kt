// !WITH_NEW_INFERENCE
fun foo(@<!UNRESOLVED_REFERENCE!>varargs<!> <!UNUSED_PARAMETER!>f<!> : Int) {}

var bar : Int = 1
  set(@<!UNRESOLVED_REFERENCE!>varargs<!> <!UNUSED_PARAMETER!>v<!>) {}

val x : (Int) -> Int = {@<!UNRESOLVED_REFERENCE!>varargs<!> <!NI;TYPE_MISMATCH, TYPE_MISMATCH, UNINITIALIZED_VARIABLE!>x<!> <!SYNTAX!>: Int -> x<!>}

class Hello(@<!UNRESOLVED_REFERENCE!>varargs<!> <!UNUSED_PARAMETER!>args<!>: Any) {
}