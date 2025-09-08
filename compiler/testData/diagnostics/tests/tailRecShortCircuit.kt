// RUN_PIPELINE_TILL: BACKEND

tailrec fun foo1(x: Int): Int {
    return maybe(x) ?: foo1(x - 1)
}

fun maybe(x: Int) = x.takeIf { x == 1 }

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun foo2(x: Int): Boolean {
    return condition(x) || <!NON_TAIL_RECURSIVE_CALL!>foo2<!>(x - 1)
}

fun condition(x: Int): Boolean = x == 0

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun Int.foo3a(): Int {
    return boo()?.<!NON_TAIL_RECURSIVE_CALL!>foo3a<!>() ?: 1
}

<!NO_TAIL_CALLS_FOUND!>tailrec<!> fun Int.foo3b(): Int {
    return boo()?.<!NON_TAIL_RECURSIVE_CALL!>foo3b<!>()?.boo() ?: 1
}

fun Int.boo(): Int? = this + 1

tailrec fun foo4(counter : Int) : Int? {
    if (counter < 0) return null
    if (counter == 0) return 777

    return <!NON_TAIL_RECURSIVE_CALL!>foo4<!>(-1) ?: <!NON_TAIL_RECURSIVE_CALL!>foo4<!>(-2) ?: foo4(counter - 1)
}

/* GENERATED_FIR_TAGS: additiveExpression, comparisonExpression, disjunctionExpression, elvisExpression,
equalityExpression, funWithExtensionReceiver, functionDeclaration, ifExpression, integerLiteral, lambdaLiteral,
nullableType, safeCall, tailrec, thisExpression */
