// SET_INT: CALL_PARAMETERS_WRAP = 4
// SET_TRUE: CALL_PARAMETERS_LPAREN_ON_NEXT_LINE

fun foo() {
    foo(bar, baz)

    foo(
            object : Quux {
                override fun foo() {
                }
            },
    )
}
