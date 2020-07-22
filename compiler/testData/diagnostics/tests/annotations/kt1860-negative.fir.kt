// !WITH_NEW_INFERENCE
fun foo(@<!OTHER_ERROR!>varargs<!> f : Int) {}

var bar : Int = 1
  set(@<!OTHER_ERROR!>varargs<!> v) {}

val x : (Int) -> Int = {@<!OTHER_ERROR!>varargs<!> x <!SYNTAX!>: Int -> x<!>}

class Hello(@<!OTHER_ERROR!>varargs<!> args: Any) {
}