fun useDeclaredVariables() {
    <!INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION!>val (<!UNRESOLVED_REFERENCE!>a<!>, <!UNRESOLVED_REFERENCE!>b<!>)<!>
    a
    b
}

fun checkersShouldRun() {
    <!INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION!>val (<!UNRESOLVED_REFERENCE!>@A a<!>, _)<!>
}

annotation class A
