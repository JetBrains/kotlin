// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

fun takeBoolean(bool: Boolean) { }
fun <T> id(t: T): T = t

fun testsWithList() {
    takeBoolean(listOf(1, 2, 3) == [1, 2, 3])
    takeBoolean([1, 2, 3] == listOf(1, 2, 3))
    id([1, 2, 3] == [1, 2, 3])
    takeBoolean(listOf(1, 2, 3) == <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    takeBoolean(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!> == listOf(1, 2, 3))
    takeBoolean([1, 2, 3] == <!CANNOT_INFER_PARAMETER_TYPE!>listOf<!>())
    id(<!CANNOT_INFER_PARAMETER_TYPE!>listOf<!>() == [1, 2, 3])
    takeBoolean(<!CANNOT_INFER_PARAMETER_TYPE!>listOf<!>() == <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    id(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!> == <!CANNOT_INFER_PARAMETER_TYPE!>listOf<!>())
    takeBoolean(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!> == <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
}

fun testsWithSet() {
    takeBoolean([1, 2, 3] == setOf(1, 2, 3))
    id(setOf(1, 2, 3) == ["!"])
    takeBoolean(<!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>() == ["!"])
}

fun testsWithIntArray(ia: IntArray) {
    if (ia == [1, 2, 3]) {
        if (ia == ["!"]) {
            if ([1, 2, 3] == ia) {
                if (ia == <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>) {
                    if (<!CANNOT_INFER_PARAMETER_TYPE!>[]<!> == ia) {
                        TODO()
                    }
                }
            }
        }
    }
}

fun testInWhenBranches() {
    when (listOf(1, 2, 3)) {
        <!CANNOT_INFER_PARAMETER_TYPE!>[]<!> -> { }
        [1, 2, 3] -> { }
        ["!"] -> { }
    }
}

fun testsWithId() {
    when ([1, 2, 3]) {
        <!CONDITION_TYPE_MISMATCH!>id(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)<!> -> { }
        id([1, 2, 3]) -> { }
        id(["!"]) -> { }
    }

    id([1, 2, 3]) == [1, 2, 3]
    id(["!"]) == id(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
}

fun testsWithNestedLiterals() {
    [[1, 2, 3]] == [[1, 2, 3]]
    [[1, 2, 3]] === [[1, 2, 3]]
    [["!"]] != <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
    <!CANNOT_INFER_PARAMETER_TYPE!>[]<!> !== [[1, 2, 3]]
    <!CANNOT_INFER_PARAMETER_TYPE!>[]<!> === <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, integerLiteral, intersectionType,
nullableType, smartcast, stringLiteral, typeParameter, whenExpression, whenWithSubject */
