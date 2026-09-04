// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-86060

fun testWithPlus() {
    [1, 2, 3] <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> [4, 5, 6]
    setOf(1, 2, 3) <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> [4, 5, 6]
    sequenceOf(1, 2, 3) <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> [4, 5, 6]
    intArrayOf(1, 2, 3) <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> [4, 5, 6]
    arrayOf(1, 2, 3) <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> [4, 5, 6]
    [1, 2, 3] as Collection<Int> <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> [4, 5, 6]
    [1, 2, 3] as Iterable<Int> <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> [4, 5, 6]
}

fun testWithMinus() {
    [1, 2, 3] <!OVERLOAD_RESOLUTION_AMBIGUITY!>-<!> [4, 5, 6]
    setOf(1, 2, 3) <!OVERLOAD_RESOLUTION_AMBIGUITY!>-<!> [4, 5, 6]
    sequenceOf(1, 2, 3) <!OVERLOAD_RESOLUTION_AMBIGUITY!>-<!> [4, 5, 6]
    [1, 2, 3] as Collection<Int> <!OVERLOAD_RESOLUTION_AMBIGUITY!>-<!> [4, 5, 6]
    [1, 2, 3] as Iterable<Int> <!OVERLOAD_RESOLUTION_AMBIGUITY!>-<!> [4, 5, 6]
}

class TestWithAssign(
    val a: MutableList<Int>,
    val b: MutableSet<Int>,
    val c: MutableCollection<Int>,
    val d: MutableIterable<Int>,
) {
    fun testPlus() {
        a <!OVERLOAD_RESOLUTION_AMBIGUITY!>+=<!> [4, 5, 6]
        b <!OVERLOAD_RESOLUTION_AMBIGUITY!>+=<!> [4, 5, 6]
        c <!OVERLOAD_RESOLUTION_AMBIGUITY!>+=<!> [4, 5, 6]
        <!VAL_REASSIGNMENT!>d<!> <!OVERLOAD_RESOLUTION_AMBIGUITY!>+=<!> [4, 5, 6]
    }

    fun testMinus() {
        a <!OVERLOAD_RESOLUTION_AMBIGUITY!>-=<!> [4, 5, 6]
        b <!OVERLOAD_RESOLUTION_AMBIGUITY!>-=<!> [4, 5, 6]
        c <!OVERLOAD_RESOLUTION_AMBIGUITY!>-=<!> [4, 5, 6]
        <!VAL_REASSIGNMENT!>d<!> <!OVERLOAD_RESOLUTION_AMBIGUITY!>-=<!> [4, 5, 6]
    }
}

fun testWithIf(cond: Boolean) {
    [1, 2, 3] <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> if (cond) [4, 5, 6] else [7, 8, 9]
    setOf(1, 2, 3) <!OVERLOAD_RESOLUTION_AMBIGUITY!>-<!> if (cond) [4, 5, 6] else [7, 8, 9]
}

fun testNestedCollections(cond: Boolean) {
    [[1, 2, 3]] <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> [4, 5, 6]
    [[1, 2, 3]] <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> [[4, 5, 6]]
    [[1, 2, 3]] <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
    [setOf(1, 2, 3)] <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> [4, 5, 6]
    [setOf(1, 2, 3)] <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> [[4, 5, 6]]
    [setOf(1, 2, 3)] <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> [setOf(4, 5, 6)]
    [setOf(1, 2, 3)] <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
    [sequenceOf(1, 2, 3)] <!OVERLOAD_RESOLUTION_AMBIGUITY!>-<!> [4, 5, 6]
    [sequenceOf(1, 2, 3)] <!OVERLOAD_RESOLUTION_AMBIGUITY!>-<!> [[4, 5, 6]]
    [sequenceOf(1, 2, 3)] <!OVERLOAD_RESOLUTION_AMBIGUITY!>-<!> [sequenceOf(4, 5, 6)]
    [sequenceOf(1, 2, 3)] <!OVERLOAD_RESOLUTION_AMBIGUITY!>-<!> <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>

    [setOf(1, 2, 3)] <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> if (cond) [] else [4, 5, 6]
    [setOf(1, 2, 3)] <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> if (cond) [[]] else [[4, 5, 6]]
    [setOf(1, 2, 3)] + if (cond) <!CANNOT_INFER_PARAMETER_TYPE!>[]<!> else <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
}

/* GENERATED_FIR_TAGS: additiveExpression, asExpression, assignment, classDeclaration, functionDeclaration, ifExpression,
integerLiteral, primaryConstructor, propertyDeclaration */
