// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE
// ISSUE: KT-84594

val a: Any
    field: Int =1

@PublishedApi
internal inline fun foo() = a.<!UNRESOLVED_REFERENCE!>inc<!>()

@PublishedApi
internal inline fun bar() {
    val local = object {
        inline fun inner() = a.<!UNRESOLVED_REFERENCE!>inc<!>()
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, explicitBackingField, functionDeclaration, inline, integerLiteral,
localProperty, propertyDeclaration, smartcast */
