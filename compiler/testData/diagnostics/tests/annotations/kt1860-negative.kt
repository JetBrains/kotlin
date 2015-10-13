fun foo(<!UNRESOLVED_REFERENCE!><!SYNTAX!><!>varargs<!> <!UNUSED_PARAMETER!>f<!> : Int) {}

var bar : Int = 1
  set(<!UNRESOLVED_REFERENCE!><!SYNTAX!><!>varargs<!> v) {}

val x : (Int) -> Int = {<!UNRESOLVED_REFERENCE!>@varargs<!> <!TYPE_MISMATCH!>x<!> <!SYNTAX!>: Int -> x<!>}

class Hello(<!UNRESOLVED_REFERENCE!><!SYNTAX!><!>varargs<!> <!UNUSED_PARAMETER!>args<!>: Any) {
}