interface A

fun test(a: A, block: A.() -> Int) {
    a.<!UNRESOLVED_REFERENCE!>block<!>()
}