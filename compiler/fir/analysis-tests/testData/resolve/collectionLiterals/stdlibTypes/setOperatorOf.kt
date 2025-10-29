// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-81722
// LANGUAGE: +CollectionLiterals

@OptIn(ExperimentalStdlibApi::class)
fun test() {
    val a: Set<String> = Set.of()
    val b: Set<Any?> = Set.of(null)
    val c: Set<Int> = Set.of(1, 2, 3)

    val x: Set<String> = []
    val y: Set<Any?> = [null]
    val z: Set<Int> = [1, 2, 3]
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, integerLiteral, localProperty, nullableType,
propertyDeclaration */
