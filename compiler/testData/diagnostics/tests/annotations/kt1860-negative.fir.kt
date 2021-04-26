// !WITH_NEW_INFERENCE
fun foo(@<!UNRESOLVED_REFERENCE!>varargs<!> f : Int) {}

var bar : Int = 1
  set(@<!UNRESOLVED_REFERENCE!>varargs<!> v) {}

val x : (Int) -> Int = <!INITIALIZER_TYPE_MISMATCH{LT}!>{@<!UNRESOLVED_REFERENCE!>varargs<!> x <!SYNTAX!>: Int -> x<!>}<!>

class Hello(@<!UNRESOLVED_REFERENCE!>varargs<!> args: Any) {
}
