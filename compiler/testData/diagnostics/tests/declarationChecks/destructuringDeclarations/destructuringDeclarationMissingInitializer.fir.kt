fun useDeclaredVariables() {
    <!INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION!>val (a, b)<!>
    a
    b
}

fun checkersShouldRun() {
    <!INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION!>val (@A a, _)<!>
}

annotation class A
