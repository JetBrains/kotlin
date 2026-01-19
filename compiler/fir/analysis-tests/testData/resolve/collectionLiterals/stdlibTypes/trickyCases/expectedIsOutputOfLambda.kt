// LANGUAGE: +CollectionLiterals
// RUN_PIPELINE_TILL: FRONTEND

fun <T> assertEq(expected: T, calc: () -> T) { }

fun <T, K> assertEqWithInput(expected: T, input: K, calc: (K) -> T) { }

fun <L> id(l: L): L = l

fun test() {
    assertEq([]) { setOf<Int>() }
    assertEq(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>) { setOf<String>() }
    assertEqWithInput([], 42) { setOf(it) }
    assertEqWithInput(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>, "") { setOf(it) }

    assertEq(id([])) { setOf<Int>() }
    assertEq(id(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)) { setOf<String>() }
    assertEqWithInput(id([]), 42) { setOf(it) }
    assertEqWithInput(id(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>), "") { setOf(it) }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, nullableType, stringLiteral,
typeParameter */
