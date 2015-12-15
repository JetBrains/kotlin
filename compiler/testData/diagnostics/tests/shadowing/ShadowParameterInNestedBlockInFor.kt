fun f(<!UNUSED_PARAMETER!>i<!>: Int) {
    for (j in 1..100) {
        <!UNUSED_LAMBDA_EXPRESSION!>{
            var <!NAME_SHADOWING, UNUSED_VARIABLE!>i<!> = 12
        }<!>
    }
}