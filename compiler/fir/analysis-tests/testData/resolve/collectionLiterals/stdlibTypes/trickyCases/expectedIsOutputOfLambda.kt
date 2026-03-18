// LANGUAGE: +CollectionLiterals
// RUN_PIPELINE_TILL: BACKEND

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

    assertEq([]) { [42] }
    assertEq([42]) { [] }

    assertEqWithInput([42], "") { [] }
    assertEqWithInput([], "") { [it] }
    assertEqWithInput(id([]), "") { id([it]) }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, nullableType, stringLiteral,
typeParameter */
