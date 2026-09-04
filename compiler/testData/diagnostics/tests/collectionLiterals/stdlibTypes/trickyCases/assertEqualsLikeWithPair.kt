// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CollectionLiterals
// FIR_DUMP

fun <T> assertEquals(a: T, b: T) = Unit

fun foo() {
    assertEquals(
        5 to setOf(1, 2, 3),
        5 to [1, 2, 3], // listOf
    )
    assertEquals(
        Pair(5, [1, 2, 3]), // listOf
        Pair(5, setOf(1, 2, 3)),
    )
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, nullableType, typeParameter */
