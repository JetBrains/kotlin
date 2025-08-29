// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class Row<T> {
    companion object {
        operator fun <T> of(vararg e: T) = Row<T>()
    }
}

class Matrix<K> {
    companion object {
        operator fun <K> of(vararg r: Row<K>) = Matrix<K>()
    }
}

fun takeMatrixInt(m: Matrix<Int>) { }
fun <U> takeMatrix(m: Matrix<U>) { }

fun testMatrixInt() {
    takeMatrixInt([
        [1, 2, 3],
        [4, 5, 6],
        [7, 8, 9],
    ])
    takeMatrixInt([])
    takeMatrixInt([[], [], []])

    takeMatrixInt([
        [1, 2, 3],
        <!ARGUMENT_TYPE_MISMATCH!>[4, '5', 6]<!>,
        [7, 8, 9],
    ])
    takeMatrixInt([
        [1, 2, 3],
        <!ARGUMENT_TYPE_MISMATCH!>[4, null, 6]<!>,
        [7, 8, 9],
    ])
    takeMatrixInt([
        [1, 2, 3],
        <!ARGUMENT_TYPE_MISMATCH!>[4, 5L, 6]<!>,
        [7, 8, 9],
    ])
    takeMatrixInt([<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>, <!ARGUMENT_TYPE_MISMATCH!>3<!>])
    takeMatrixInt([[<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>]])

    testMatrix<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>(<!TOO_MANY_ARGUMENTS, UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[
        <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>,
        <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[4, 5, 6]<!>,
        <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[7, 8, 9]<!>,
    ]<!>)
    takeMatrix<Int>([])
    takeMatrix<Int>([[], [], []])

    takeMatrix<Int>([
        [1, 2, 3],
        <!ARGUMENT_TYPE_MISMATCH!>[4, '5', 6]<!>,
        [7, 8, 9],
    ])
    takeMatrix<Int>([
        [1, 2, 3],
        <!ARGUMENT_TYPE_MISMATCH!>[4, null, 6]<!>,
        [7, 8, 9],
    ])
    takeMatrix<Int>([
        [1, 2, 3],
        <!ARGUMENT_TYPE_MISMATCH!>[4, 5L, 6]<!>,
        [7, 8, 9],
    ])
    takeMatrix<Int>([<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>, <!ARGUMENT_TYPE_MISMATCH!>3<!>])
    takeMatrix<Int>([[<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>]])

    var matrix: Matrix<Int> = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]
    matrix = []
    matrix = [[]]
    matrix = [[<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>]]
    matrix = [[<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>]]
    matrix = [<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>, <!ARGUMENT_TYPE_MISMATCH!>3<!>]
    matrix = [[1, null, 3]]
    matrix = [[1, "2", 3]]
    matrix = [[null!!]]
}

fun testMatrix() {
    <!CANNOT_INFER_PARAMETER_TYPE!>takeMatrix<!>(<!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>[]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeMatrix<!>(<!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>[<!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>[]<!>]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeMatrix<!>(<!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>[<!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>[1]<!>]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeMatrix<!>(<!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>[<!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>[<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>]<!>]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeMatrix<!>(<!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>[1]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeMatrix<!>(<!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>[
        <!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>[1, 2, 3]<!>,
        <!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>[4, '5', 6]<!>,
        <!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>[7, 8, 9]<!>,
    ]<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, integerLiteral, intersectionType,
nullableType, objectDeclaration, operator, typeParameter, vararg */
