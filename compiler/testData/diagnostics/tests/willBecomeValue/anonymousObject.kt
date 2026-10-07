// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalValueClassesApi

val anonymous = <!WILL_BECOME_VALUE_NOT_APPLICABLE!>@WillBecomeValue<!> object {
    override fun toString(): String = "anonymous"
}

fun local() = <!WILL_BECOME_VALUE_NOT_APPLICABLE!>@WillBecomeValue<!> object {}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, override, propertyDeclaration, stringLiteral */
