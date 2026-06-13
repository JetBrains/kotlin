// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-30054
// LANGUAGE_FEATURE_TOGGLED: KeepNullabilityWhenApproximatingLocalType
// LANGUAGE_FEATURE_TOGGLED_IDENTICAL
interface I {
    fun foo(): String
}

fun bar(condition: Boolean) /*: I? */ =
    if (condition)
        object : I {
            override fun foo() = "should check for null first"
            fun baz() = "invisible"
        }
    else null

fun main() {
    bar(false).<!UNRESOLVED_REFERENCE!>baz<!>()
    bar(false)<!UNSAFE_CALL!>.<!>foo()
    bar(false)?.foo()
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, functionDeclaration, ifExpression, interfaceDeclaration, nullableType,
override, safeCall, stringLiteral */
