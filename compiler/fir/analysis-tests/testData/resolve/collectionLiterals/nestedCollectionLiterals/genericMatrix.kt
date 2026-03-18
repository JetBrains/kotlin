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
    takeMatrixInt([[<!ARGUMENT_TYPE_MISMATCH!>"1"<!>, <!ARGUMENT_TYPE_MISMATCH!>"2"<!>, <!ARGUMENT_TYPE_MISMATCH!>"3"<!>]])

    takeMatrixInt([
        [1, 2, 3],
        [4, <!ARGUMENT_TYPE_MISMATCH!>'5'<!>, 6],
        [7, 8, 9],
    ])
    takeMatrixInt([
        [1, 2, 3],
        [4, <!NULL_FOR_NONNULL_TYPE!>null<!>, 6],
        [7, 8, 9],
    ])
    takeMatrixInt([
        [1, 2, 3],
        [4, <!ARGUMENT_TYPE_MISMATCH!>5L<!>, 6],
        [7, 8, 9],
    ])
    takeMatrixInt([<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>, <!ARGUMENT_TYPE_MISMATCH!>3<!>])
    takeMatrixInt([[<!UNRESOLVED_REFERENCE!>[1, 2, 3]<!>]])

    takeMatrix<Int>([
        [1, 2, 3],
        [4, 5, 6],
        [7, 8, 9],
    ])
    takeMatrix<Int>([])
    takeMatrix<Int>([[], [], []])

    takeMatrix<Int>([
        [1, 2, 3],
        [4, <!ARGUMENT_TYPE_MISMATCH!>'5'<!>, 6],
        [7, 8, 9],
    ])
    takeMatrix<Int>([
        [1, 2, 3],
        [4, <!NULL_FOR_NONNULL_TYPE!>null<!>, 6],
        [7, 8, 9],
    ])
    takeMatrix<Int>([
        [1, 2, 3],
        [4, <!ARGUMENT_TYPE_MISMATCH!>5L<!>, 6],
        [7, 8, 9],
    ])
    takeMatrix<Int>([<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>, <!ARGUMENT_TYPE_MISMATCH!>3<!>])
    takeMatrix<Int>([[<!UNRESOLVED_REFERENCE!>[1, 2, 3]<!>]])

    var matrix: Matrix<Int> = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]
    matrix = []
    matrix = [[]]
    matrix = [[<!UNRESOLVED_REFERENCE!>[]<!>]]
    matrix = [[<!UNRESOLVED_REFERENCE!>[1, 2, 3]<!>]]
    matrix = [<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>, <!ARGUMENT_TYPE_MISMATCH!>3<!>]
    matrix = [[1, <!NULL_FOR_NONNULL_TYPE!>null<!>, 3]]
    matrix = [[1, <!ARGUMENT_TYPE_MISMATCH!>"2"<!>, 3]]
    matrix = [[null!!]]
    matrix = [[<!ARGUMENT_TYPE_MISMATCH!>"1"<!>, <!ARGUMENT_TYPE_MISMATCH!>"2"<!>, <!ARGUMENT_TYPE_MISMATCH!>"3"<!>]]
    val matrixString: Matrix<String> = [[<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>, <!ARGUMENT_TYPE_MISMATCH!>3<!>]]
}

fun testMatrix() {
    <!CANNOT_INFER_PARAMETER_TYPE!>takeMatrix<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeMatrix<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>]<!>)
    takeMatrix([[1]])
    <!CANNOT_INFER_PARAMETER_TYPE!>takeMatrix<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[<!CANNOT_INFER_PARAMETER_TYPE!>[<!UNRESOLVED_REFERENCE!>[]<!>]<!>]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeMatrix<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[<!ARGUMENT_TYPE_MISMATCH!>1<!>]<!>)
    takeMatrix([
        [1, 2, 3],
        [4, '5', 6],
        [7, 8, 9],
    ])
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, integerLiteral, intersectionType,
nullableType, objectDeclaration, operator, typeParameter, vararg */
