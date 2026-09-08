// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP

fun test() {
    listOf([42]).contains([42])
    listOf(setOf(42)).contains([42])
    listOf(Any()).contains([42])

    sequenceOf([42]).contains([42])
    sequenceOf(setOf(42)).contains([42])
    sequenceOf(Any()).contains([42])
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral */
