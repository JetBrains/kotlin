// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-81722
// LANGUAGE: +CollectionLiterals

@OptIn(ExperimentalStdlibApi::class)
fun test() {
    val a: List<String> = List.of()
    val b: List<Any?> = List.of(null)
    val c: List<Int> = List.of(1, 2, 3)

    val x: List<String> = []
    val y: List<Any?> = [null]
    val z: List<Int> = [1, 2, 3]
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, integerLiteral, localProperty, nullableType,
propertyDeclaration */
