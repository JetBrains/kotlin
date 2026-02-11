// LANGUAGE: +CollectionLiterals
// RUN_PIPELINE_TILL: FRONTEND

fun <T> assertEq(expected: T, calc: () -> T) { }

fun <T, K> assertEqWithInput(expected: T, input: K, calc: (K) -> T) { }

fun <L> id(l: L): L = l

fun test() {
    assertEq([]) { setOf<Int>() }
    assertEq([42]) { setOf<String>() }
    assertEqWithInput([], 42) { setOf(it) }
    assertEqWithInput([42], "") { setOf(it) }

    assertEq(id([])) { setOf<Int>() }
    assertEq(id([42])) { setOf<String>() }
    assertEqWithInput(id([]), 42) { setOf(it) }
    assertEqWithInput(id([42]), "") { setOf(it) }

    <!CANNOT_INFER_PARAMETER_TYPE!>assertEq<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>) { <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42]<!> }
    <!CANNOT_INFER_PARAMETER_TYPE!>assertEq<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42]<!>) { <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!> }

    <!CANNOT_INFER_PARAMETER_TYPE!>assertEqWithInput<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42]<!>, "") { <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!> }
    <!CANNOT_INFER_PARAMETER_TYPE!>assertEqWithInput<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>, "") { <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[it]<!> }
    <!CANNOT_INFER_PARAMETER_TYPE!>assertEqWithInput<!>(<!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>), "") { <!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[it]<!>) }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, nullableType, stringLiteral,
typeParameter */
