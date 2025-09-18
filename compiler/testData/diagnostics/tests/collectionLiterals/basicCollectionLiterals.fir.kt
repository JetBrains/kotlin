// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE
// DIAGNOSTICS: -UNUSED_VARIABLE, -UNSUPPORTED

fun test() {
    val a = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>
    val b: Array<Int> = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!>
    val c = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[1, 2]<!>
    val d: Array<Int> = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[1, 2]<!>
    val e: Array<String> = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[1]<!>

    val f: IntArray = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[1, 2]<!>
    val g = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[f]<!>
}

fun check() {
    <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[1, 2]<!> checkType { _<Array<Int>>() }
    <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[""]<!> checkType { _<Array<String>>() }

    val f: IntArray = <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[1]<!>
    <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[f]<!> checkType { _<Array<IntArray>>() }

    <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[1, ""]<!> checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Array<Any>>() }
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, funWithExtensionReceiver, functionDeclaration,
functionalType, infix, integerLiteral, intersectionType, lambdaLiteral, localProperty, nullableType, outProjection,
propertyDeclaration, stringLiteral, typeParameter, typeWithExtension */
