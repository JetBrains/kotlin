// LANGUAGE: +CollectionLiterals
// DUMP_CFG
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun test() {
    val cl1 = [1, 2, 3]
}

fun testNested() {
    val cl2: Set<*> = [[1], [2, 3]]
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, localProperty, propertyDeclaration */
