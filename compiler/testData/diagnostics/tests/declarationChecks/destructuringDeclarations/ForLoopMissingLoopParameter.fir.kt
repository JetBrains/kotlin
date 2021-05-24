fun useDeclaredVariables() {
    for ((a, b)<!SYNTAX!><!>) {
        a
        b
    }
}

fun checkersShouldRun() {
    for ((@A a, _)<!SYNTAX!><!>) {

    }
}

annotation class A
