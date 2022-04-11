fun foo(action: () -> Unit = {}): Int = 0

fun usageResolved1() {
    foo().<!SYNTAX!><!>
}

fun usageResolved2() {
    foo()?.<!SYNTAX!><!>
}

fun usageResolved3() {
    foo {
        foo()
    }.<!SYNTAX!><!>
}

fun usageUnresolved1() {
    <!UNRESOLVED_REFERENCE!>bar<!>().<!SYNTAX!><!>
}

fun usageUnresolved2() {
    <!UNRESOLVED_REFERENCE!>bar<!>()?.<!SYNTAX!><!>
}

fun usageUnresolved3() {
    foo {
        <!UNRESOLVED_REFERENCE!>bar<!>()
    }.<!SYNTAX!><!>
}

