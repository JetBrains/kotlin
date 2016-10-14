fun useDeclaredVariables() {
    for ((a, b)<!SYNTAX!><!>) {
        <!UNUSED_EXPRESSION, DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>
        <!UNUSED_EXPRESSION, DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>b<!>
    }
}

fun checkersShouldRun() {
    for ((@A a, <!UNDERSCORE_IS_RESERVED, UNUSED_VARIABLE!>_<!>)<!SYNTAX!><!>) {

    }
}

annotation class A