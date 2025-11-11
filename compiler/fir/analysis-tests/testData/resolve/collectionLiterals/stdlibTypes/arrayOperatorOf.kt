// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-81722
// LANGUAGE: +CollectionLiterals

@OptIn(ExperimentalCollectionLiterals::class)
fun test() {
    val a: Array<String> = Array.of()
    val b: Array<Any?> = Array.of(null)
    val c: Array<Int> = Array.of(1, 2, 3)

    val x: Array<String> = []
    val y: Array<Any?> = [null]
    val z: Array<Int> = [1, 2, 3]
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, integerLiteral, localProperty, nullableType,
propertyDeclaration */
