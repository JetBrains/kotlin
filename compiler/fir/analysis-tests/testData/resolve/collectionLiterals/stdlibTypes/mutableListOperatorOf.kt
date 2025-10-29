// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-81722
// LANGUAGE: +CollectionLiterals

@OptIn(ExperimentalStdlibApi::class)
fun test() {
    val a: MutableList<String> = MutableList.of()
    val b: MutableList<Any?> = MutableList.of(null)
    val c: MutableList<Int> = MutableList.of(1, 2, 3)

    val x: MutableList<String> = []
    val y: MutableList<Any?> = [null]
    val z: MutableList<Int> = [1, 2, 3]

    x.add("hello")
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, integerLiteral, localProperty, nullableType,
propertyDeclaration, stringLiteral */
