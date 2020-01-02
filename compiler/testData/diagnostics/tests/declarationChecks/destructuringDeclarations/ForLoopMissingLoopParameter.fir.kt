// !WITH_NEW_INFERENCE
fun useDeclaredVariables() {
    <!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>for ((<!UNRESOLVED_REFERENCE!>a<!>, <!UNRESOLVED_REFERENCE!>b<!>)<!SYNTAX!><!>) {
        a
        b
    }<!>
}

fun checkersShouldRun() {
    <!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>for ((<!UNRESOLVED_REFERENCE!>@A a<!>, <!UNRESOLVED_REFERENCE!>_<!>)<!SYNTAX!><!>) {

    }<!>
}

annotation class A
