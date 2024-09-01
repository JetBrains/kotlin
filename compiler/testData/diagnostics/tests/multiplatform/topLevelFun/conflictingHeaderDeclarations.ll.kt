// MODULE: m1-common
// FILE: common.kt

expect <!CONFLICTING_OVERLOADS!>fun foo()<!>
expect <!CONFLICTING_OVERLOADS!>fun foo()<!>

expect fun foo(x: Int)
