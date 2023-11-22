fun useDeclaredVariables() {
    <!ITERATOR_MISSING!>for ((a, b)<!SYNTAX!><!>) {
        a
        b
    }<!>
}

fun checkersShouldRun() {
    <!ITERATOR_MISSING!>for ((@A a, _)<!SYNTAX!><!>) {

    }<!>
}

annotation class A
