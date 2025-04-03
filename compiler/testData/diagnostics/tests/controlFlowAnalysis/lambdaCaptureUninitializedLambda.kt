// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUES: KT-74326

fun test1() {
    val firstLambda: (Int) -> Int
    val secondLambda: (Int) -> Int

    firstLambda = { x -> <!UNINITIALIZED_VARIABLE!>secondLambda<!>(x) }
    secondLambda = { y -> firstLambda(y) }
}

fun test2() {
    val firstLambda: (Int) -> Int
    val secondLambda: (Int) -> Int

    firstLambda = { x -> <!UNINITIALIZED_VARIABLE!>secondLambda<!>.invoke(x) }
    secondLambda = { y -> firstLambda.invoke(y) }
}
