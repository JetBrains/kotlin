fun useDeclaredVariables() {
    for ((a, b)<!SYNTAX!><!>) {
        <!UNUSED_EXPRESSION, DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>
        <!UNUSED_EXPRESSION, DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>b<!>
    }
}

fun checkersShouldRun() {
    for ((@A <!UNUSED_VARIABLE!>a<!>, _)<!SYNTAX!><!>) {

    }
}

annotation class A
