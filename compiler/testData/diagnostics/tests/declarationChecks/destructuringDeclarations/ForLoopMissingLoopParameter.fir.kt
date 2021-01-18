// !WITH_NEW_INFERENCE
fun useDeclaredVariables() {
    <!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>for ((a, b)<!SYNTAX!><!>) {
        a
        b
    }<!>
}

fun checkersShouldRun() {
    <!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>for ((@A a, _)<!SYNTAX!><!>) {

    }<!>
}

annotation class A
