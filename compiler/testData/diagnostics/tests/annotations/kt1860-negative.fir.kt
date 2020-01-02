// !WITH_NEW_INFERENCE
fun foo(@varargs f : Int) {}

var bar : Int = 1
  set(@varargs v) {}

val x : (Int) -> Int = {@varargs x <!SYNTAX!>: Int -> x<!>}

class Hello(@varargs args: Any) {
}