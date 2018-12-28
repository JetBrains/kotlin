fun useDeclaredVariables() {
    val (a, b) = <!UNRESOLVED_REFERENCE!>unresolved<!>
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, UNUSED_EXPRESSION!>a<!>
    <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, UNUSED_EXPRESSION!>b<!>
}

fun checkersShouldRun() {
    val (@A <!UNUSED_VARIABLE!>a<!>, _) = <!UNRESOLVED_REFERENCE!>unresolved<!>
}

annotation class A
