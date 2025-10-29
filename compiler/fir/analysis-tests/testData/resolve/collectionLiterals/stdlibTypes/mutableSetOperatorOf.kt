// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-81722
// LANGUAGE: +CollectionLiterals

@OptIn(ExperimentalStdlibApi::class)
fun test() {
    val a: MutableSet<String> = MutableSet.of()
    val b: MutableSet<Any?> = MutableSet.of(null)
    val c: MutableSet<Int> = MutableSet.of(1, 2, 3)

    val x: MutableSet<String> = []
    val y: MutableSet<Any?> = [null]
    val z: MutableSet<Int> = [1, 2, 3]

    x.add("hello")
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, integerLiteral, localProperty, nullableType,
propertyDeclaration, stringLiteral */
