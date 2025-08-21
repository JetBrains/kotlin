// RUN_PIPELINE_TILL: BACKEND
val x = object {
    fun foo(types: List<String>) {
        val length = "123"
        types.mapIndexed { i, length -> Triple(i, length, length.getFilteredType()) }
    }

    private fun String.getFilteredType() = bar(length)
}

fun bar(x: Int) = x

/* GENERATED_FIR_TAGS: anonymousObjectExpression, funWithExtensionReceiver, functionDeclaration, lambdaLiteral,
localProperty, propertyDeclaration, stringLiteral */
