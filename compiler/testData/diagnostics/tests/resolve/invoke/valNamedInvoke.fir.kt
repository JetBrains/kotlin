interface A

fun foo(invoke: A.()->Unit, a: A) {
    a.<!UNRESOLVED_REFERENCE!>invoke<!>()
}

fun bar(invoke: Any.()->Any, a: Any) {
    a.<!UNRESOLVED_REFERENCE!>invoke<!>()
}
