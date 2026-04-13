// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-47626
// WITH_STDLIB

// KT-47626: False-positive "Returning type parameter has been inferred to Nothing implicitly" warning

fun foo(cause: Throwable?): Int = if (true) 42 else cause?.let { throw it } ?: TODO()

@Suppress("MayBeConstant")
private val SUPPORT_MISSING = true

private fun createMissingDispatcher(cause: Throwable? = null, errorHint: String? = null): Any? =
    if (SUPPORT_MISSING) Any() else
        cause?.let { throw it } ?: run { throw UnsupportedOperationException() }

private fun foo2(value: String?): String {
    value?.let { _ ->
        return ""
    }
    return ""
}

/* GENERATED_FIR_TAGS: elvisExpression, functionDeclaration, ifExpression, integerLiteral, lambdaLiteral, nullableType,
propertyDeclaration, safeCall, stringLiteral */
