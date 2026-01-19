// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

fun <T, V> idWithSetUB(t: T): T where T : Set<V> = t
fun <T, V> idWithMutableSetUB(t: T): T where T : MutableSet<V> = t

interface BoxWithSet<X> {
    var set: Set<X>
}

interface BoxWithMutableSet<X> {
    var set: MutableSet<X>
}

fun <Y> buildBoxWithSet(block: BoxWithSet<Y>.() -> Unit) { }

fun <Z> buildBoxWithMutableSet(block: BoxWithMutableSet<Z>.() -> Unit) { }

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE!>buildBoxWithSet<!> {
        set = <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>idWithSetUB<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    }

    <!CANNOT_INFER_PARAMETER_TYPE!>buildBoxWithSet<!> {
        set = <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>idWithSetUB<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>)
    }

    <!CANNOT_INFER_PARAMETER_TYPE!>buildBoxWithSet<!> {
        set = <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>idWithSetUB<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42]<!>)
        set = <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>idWithSetUB<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["42"]<!>)
    }

    <!CANNOT_INFER_PARAMETER_TYPE!>buildBoxWithMutableSet<!> {
        set = <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>idWithMutableSetUB<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    }

    <!CANNOT_INFER_PARAMETER_TYPE!>buildBoxWithMutableSet<!> {
        set = <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>idWithMutableSetUB<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>)
    }

    <!CANNOT_INFER_PARAMETER_TYPE!>buildBoxWithMutableSet<!> {
        set = <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>idWithMutableSetUB<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42]<!>)
        set = <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>idWithMutableSetUB<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["42"]<!>)
    }
}

/* GENERATED_FIR_TAGS: assignment, collectionLiteral, functionDeclaration, functionalType, integerLiteral,
interfaceDeclaration, lambdaLiteral, nullableType, propertyDeclaration, stringLiteral, typeConstraint, typeParameter,
typeWithExtension */
