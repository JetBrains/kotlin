// !WITH_NEW_INFERENCE
fun useDeclaredVariables() {
    for ((a, b)<!SYNTAX!><!>) {
        <!OI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, UNUSED_EXPRESSION!>a<!>
        <!OI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, UNUSED_EXPRESSION!>b<!>
    }
}

fun checkersShouldRun() {
    for ((@A <!UNUSED_VARIABLE!>a<!>, _)<!SYNTAX!><!>) {

    }
}

annotation class A
