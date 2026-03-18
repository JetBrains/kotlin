// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class Row {
    companion object {
        operator fun of(vararg e: Int) = Row()
    }
}

class Matrix {
    companion object {
        operator fun of(vararg r: Row) = Matrix()
    }
}

fun takeMatrix(m: Matrix) { }

fun test() {
    takeMatrix([
       [1, 2, 3],
       [4, 5, 6],
       [7, 8, 9],
    ])
    takeMatrix([])
    takeMatrix([[], [], []])

    takeMatrix([
        [1, 2, 3],
        [4, <!ARGUMENT_TYPE_MISMATCH!>'5'<!>, 6],
        [7, 8, 9],
    ])
    takeMatrix([
       [1, 2, 3],
       [4, <!NULL_FOR_NONNULL_TYPE!>null<!>, 6],
       [7, 8, 9],
    ])
    takeMatrix([
       [1, 2, 3],
       [4, <!ARGUMENT_TYPE_MISMATCH!>5L<!>, 6],
       [7, 8, 9],
    ])
    takeMatrix([<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>, <!ARGUMENT_TYPE_MISMATCH!>3<!>])
    takeMatrix([[<!UNRESOLVED_REFERENCE!>[1, 2, 3]<!>]])

    var matrix: Matrix = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]
    matrix = []
    matrix = [[]]
    matrix = [<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>, <!ARGUMENT_TYPE_MISMATCH!>3<!>]
    matrix = [[<!ARGUMENT_TYPE_MISMATCH!>"1"<!>, <!ARGUMENT_TYPE_MISMATCH!>"2"<!>, <!ARGUMENT_TYPE_MISMATCH!>"3"<!>]]
    matrix = [[<!UNRESOLVED_REFERENCE!>[]<!>]]
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, integerLiteral, objectDeclaration,
operator, stringLiteral, vararg */
