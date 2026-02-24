// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +CollectionLiterals

class Matrix<out T>(private vararg val rows: Matrix.Row<T>) {
    class Row<out T>(private vararg val values: T) {
        companion object {
            operator fun <X> of(vararg values: X) = Row(*values)
        }
    }
    companion object {
        operator fun <X> of(vararg values: Row<X>) = Matrix(*values)
    }
}

fun f1(x: Matrix<Int>) { }
fun f1(x: Matrix<String>) { }

fun t1() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>f1<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>]<!>) // ambiguity
    f1([["!"]]) // string
    f1([[1], [2], [3]]) // int
    f1([[1], Matrix.Row(2), [3]]) // int
    f1([[1], Matrix.Row(), [3]]) // int
    <!NONE_APPLICABLE!>f1<!>([["1"], Matrix.Row(2), ["3"]]) // no applicable?
    f1([["1"], Matrix.Row<String>(), ["3"]]) // string
}

fun f2(x: Matrix<Int>) { }
fun f2(x: Matrix<Any>) { }

fun t2() {
    f2([[]]) // int
    f2([["!"]]) // any
    f2([[1], [2], [3]]) // int
    f2([["1"], ["2"], ["3"]]) // any
    f2([[1], Matrix.Row(2), [3]]) // int
    f2([[1], Matrix.Row(), [3]]) // int
    f2([[1], Matrix.Row<String>(), [3]]) // any
    f2([["1"], Matrix.Row(2), ["3"]]) // any
    f2([["1"], Matrix.Row("2"), ["3"]]) // any
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, integerLiteral, intersectionType,
nestedClass, nullableType, objectDeclaration, operator, out, outProjection, primaryConstructor, propertyDeclaration,
stringLiteral, typeParameter, vararg */
