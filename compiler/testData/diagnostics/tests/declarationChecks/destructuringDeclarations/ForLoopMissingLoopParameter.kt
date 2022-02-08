fun useDeclaredVariables() {
    for ((a, b)<!SYNTAX!><!>) {
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>b<!>
    }
}

fun checkersShouldRun() {
    for ((@A a, _)<!SYNTAX!><!>) {

    }
}

annotation class A
