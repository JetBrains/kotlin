// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-68667

fun foo(list: List<List<String>?>) {
    val some = list.mapNotNull { it ?: arrayListOf() }
}

/* GENERATED_FIR_TAGS: elvisExpression, functionDeclaration, lambdaLiteral, localProperty, nullableType,
propertyDeclaration */
