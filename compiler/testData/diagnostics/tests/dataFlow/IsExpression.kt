fun f(<!UNUSED_PARAMETER!>a<!>: Boolean, <!UNUSED_PARAMETER!>b<!>: Int) {}

fun foo(a: Any) {
    f(a is Int, <!TYPE_MISMATCH!>a<!>)
    1 <!NONE_APPLICABLE!>+<!> a
}