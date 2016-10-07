fun useDeclaredVariables() {
    val (a, b) = <!UNRESOLVED_REFERENCE!>unresolved<!>
    <!UNUSED_EXPRESSION, DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>
    <!UNUSED_EXPRESSION, DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>b<!>
}

fun checkersShouldRun() {
    val (@A a, <!UNDERSCORE_IS_RESERVED, UNUSED_VARIABLE!>_<!>) = <!UNRESOLVED_REFERENCE!>unresolved<!>
}

annotation class A