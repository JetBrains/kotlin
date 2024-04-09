// LANGUAGE: +ProhibitSimplificationOfNonTrivialConstBooleanExpressions
fun test() {
    <!WRONG_ANNOTATION_TARGET!>@ann<!>
    while (2 < 1) {}

    <!WRONG_ANNOTATION_TARGET!>@ann<!>
    do {} while (2 < 1)

    @ann
    for (i in 1..2) {}
}

annotation class ann
