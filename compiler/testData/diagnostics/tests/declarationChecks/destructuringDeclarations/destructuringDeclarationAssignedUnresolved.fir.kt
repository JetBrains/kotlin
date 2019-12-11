fun useDeclaredVariables() {
    val (<!UNRESOLVED_REFERENCE!>a<!>, <!UNRESOLVED_REFERENCE!>b<!>) = <!UNRESOLVED_REFERENCE!>unresolved<!>
    a
    b
}

fun checkersShouldRun() {
    val (<!UNRESOLVED_REFERENCE!>@A a<!>, <!UNRESOLVED_REFERENCE!>_<!>) = <!UNRESOLVED_REFERENCE!>unresolved<!>
}

annotation class A
