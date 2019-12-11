fun useDeclaredVariables() {
    val (<!UNRESOLVED_REFERENCE!>a<!>, <!UNRESOLVED_REFERENCE!>b<!>)
    a
    b
}

fun checkersShouldRun() {
    val (<!UNRESOLVED_REFERENCE!>@A a<!>, <!UNRESOLVED_REFERENCE!>_<!>)
}

annotation class A
