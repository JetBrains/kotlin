annotation class test

fun foo(test <!UNUSED_PARAMETER!>f<!> : Int) {}

var bar : Int = 1
    set(test v) {}

val x : (Int) -> Int = {[test] <!TYPE_MISMATCH!>x<!> <!DEPRECATED_STATIC_ASSERT!>: Int<!> <!SYNTAX!>-> x<!>} // todo fix parser annotation on lambda parameter

class Hello(test <!UNUSED_PARAMETER!>args<!>: Any) {
}
