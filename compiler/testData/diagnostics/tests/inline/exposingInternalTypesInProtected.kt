// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE
// LANGUAGE: -ForbidExposingLessVisibleTypesInInline

class C {
    protected inline fun foo(x: Any) {
        <!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING, NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>Internal<!>()
        x is <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Internal<!>
        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Internal<!>::class

        Published()
        x is Published
        Published::class
    }
}

internal class Internal

@PublishedApi
internal class Published

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, inline, isExpression */
