// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE, -UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR, -UNSUPPORTED

annotation class Anno(val a: Array<String> = [""], val b: IntArray = [])

@Anno([], [])
fun test() {}

fun arrayOf(): Array<Int> = TODO()
fun intArrayOf(): Array<Int> = TODO()

fun local() {
    val a1: IntArray = [1, 2]
    val a2: IntArray = []

    val s1: Array<String> = [""]
    val s2: Array<String> = []
}

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, functionDeclaration, integerLiteral, localProperty,
primaryConstructor, propertyDeclaration, stringLiteral */
