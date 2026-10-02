// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -CollectionLiterals +CompanionBlocks
// WITH_STDLIB

fun test() {
    val s0: Sequence<Int> = Sequence.<!OPT_IN_USAGE_ERROR!>of<!>()
    val s1 = Sequence.<!OPT_IN_USAGE_ERROR!>of<!>(42)
    val s3 = Sequence.<!OPT_IN_USAGE_ERROR!>of<!>('a', 'b', 'c')
}

@OptIn(ExperimentalCollectionLiteralsApi::class)
fun testWithOptIn() {
    val s0: Sequence<Int> = Sequence.of()
    val s1 = Sequence.of(42)
    val s3 = Sequence.of('a', 'b', 'c')
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, integerLiteral, localProperty, propertyDeclaration */
