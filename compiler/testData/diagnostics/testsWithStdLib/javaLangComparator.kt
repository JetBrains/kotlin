// RUN_PIPELINE_TILL: CODEGEN
fun test_2(list: List<Int>) {
    val comp = java.util.Comparator<Int> { x, y -> 1 }
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration */
