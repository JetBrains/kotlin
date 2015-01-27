annotation class test

fun foo(test <!UNUSED_PARAMETER!>f<!> : Int) {}

var bar : Int = 1
    set(test v) {}

val x : (Int) -> Int = {([test] x : Int) -> x}

class Hello(test <!UNUSED_PARAMETER!>args<!>: Any) {
}
