// ISSUE: KT-62018, KT-62019

fun take(f: suspend () -> Unit) {}

fun test_1() {
    <!ANONYMOUS_SUSPEND_FUNCTION!>suspend<!> fun() {}
}

fun test_2() {
    take(<!UNRESOLVED_REFERENCE!>suspend<!><!SYNTAX!><!> <!TOO_MANY_ARGUMENTS!>fun () {}<!>)
}
