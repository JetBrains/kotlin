// SET_INT: CALL_PARAMETERS_WRAP = 2
// SET_TRUE: ALIGN_MULTILINE_PARAMETERS_IN_CALLS
// SET_TRUE: CALL_PARAMETERS_LPAREN_ON_NEXT_LINE
// RIGHT_MARGIN: 30

fun foo() {
    testtest()

    testtesttesttesttesttesttesttesttest()

    testtest(foofoo)

    testtesttesttest(foofoofoofoofoofoofoofoofoofoofoofoo)

    testtest(
            foobar,
            barfoo)

    testtesttesttest(
            foofoo,
            barbar,
            foobar,
            barfoo)
}