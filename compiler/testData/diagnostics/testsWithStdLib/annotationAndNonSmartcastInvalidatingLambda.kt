// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE_FEATURE_TOGGLED: CollectionLiterals
// LANGUAGE_FEATURE_TOGGLED_IDENTICAL

fun cond(): Boolean = true
fun materialize(): Any = Any()

fun main() {
    var throwable: Throwable? = null

    @Suppress("...")
    when {
        cond() -> {
            try {
                run {
                    throwable = materialize() as Throwable
                }
            } catch (_: Exception) { }
        }
    }

    if (throwable != null) {
        throw throwable
    }
}

/* GENERATED_FIR_TAGS: asExpression, assignment, equalityExpression, functionDeclaration, ifExpression, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, smartcast, stringLiteral, tryExpression, unnamedLocalVariable,
whenExpression */
