// RUN_PIPELINE_TILL: FRONTEND
// COMPARE_WITH_LIGHT_TREE
fun foo(@<!UNRESOLVED_REFERENCE!>varargs<!> f : Int) {}

var bar : Int = 1
  set(@<!UNRESOLVED_REFERENCE!>varargs<!> v) {}

val x : (Int) -> Int = {@<!UNRESOLVED_REFERENCE!>varargs<!> <!RETURN_TYPE_MISMATCH!>x<!> <!SYNTAX!>: Int -> x<!>}

class Hello(@<!UNRESOLVED_REFERENCE!>varargs<!> args: Any) {
}
