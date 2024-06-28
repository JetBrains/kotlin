fun useDeclaredVariables() {
    val (a, b) = <!COMPONENT_FUNCTION_MISSING, COMPONENT_FUNCTION_MISSING, UNRESOLVED_REFERENCE!>unresolved<!>
    a
    b
}

fun checkersShouldRun() {
    val (@A a, _) = <!COMPONENT_FUNCTION_MISSING, COMPONENT_FUNCTION_MISSING, UNRESOLVED_REFERENCE!>unresolved<!>
}

annotation class A
