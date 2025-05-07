// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE
// LANGUAGE: -ForbidExposingLessVisibleTypesInInline

class C {
    protected inline fun foo(x: Any) {
        <!LESS_VISIBLE_CONTAINING_CLASS_IN_INLINE_WARNING, LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING, NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>Internal<!>()
        x is <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Internal<!>

        Published()
        x is Published
    }
}

internal class Internal

@PublishedApi
internal class Published
