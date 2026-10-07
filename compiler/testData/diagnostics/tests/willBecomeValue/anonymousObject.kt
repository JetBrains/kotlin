// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalValueClassesApi

val anonymous = @WillBecomeValue object {
    override fun toString(): String = "anonymous"
}

fun local() = @WillBecomeValue object {}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, override, propertyDeclaration, stringLiteral */
