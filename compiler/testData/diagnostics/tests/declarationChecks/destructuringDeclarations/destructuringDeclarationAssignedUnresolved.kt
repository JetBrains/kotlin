fun useDeclaredVariables() {
    val (a, b) = <!UNRESOLVED_REFERENCE!>unresolved<!>
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>b<!>
}

fun checkersShouldRun() {
    val (@A a, _) = <!UNRESOLVED_REFERENCE!>unresolved<!>
}

annotation class A
