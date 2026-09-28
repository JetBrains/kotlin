// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// ISSUE: KT-64066

fun box() {
    val map = buildMap {
        put(1, 1)
        for (v in values) {}
    }
}

/* GENERATED_FIR_TAGS: forLoop, functionDeclaration, integerLiteral, lambdaLiteral, localProperty, nullableType,
propertyDeclaration */
