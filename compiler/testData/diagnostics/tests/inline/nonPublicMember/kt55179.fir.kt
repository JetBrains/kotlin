// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-55179
// LANGUAGE: +ForbidExposingLessVisibleTypesInInline
// SKIP_TXT
// RENDER_DIAGNOSTICS_FULL_TEXT

private class Foo {
    companion object {
        fun buildFoo() = Foo()

        object Nested {
            fun bar() {}
        }
    }
}

internal <!NOTHING_TO_INLINE!>inline<!> fun foo() {
    <!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_ERROR, PRIVATE_CLASS_MEMBER_FROM_INLINE!>Foo<!>()
    Foo.<!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR, PRIVATE_CLASS_MEMBER_FROM_INLINE!>Companion<!>
    <!PRIVATE_CLASS_MEMBER_FROM_INLINE!>Foo<!>.<!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_ERROR, LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_ERROR, PRIVATE_CLASS_MEMBER_FROM_INLINE!>buildFoo<!>()
    Foo.Companion.Nested.<!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_ERROR!>bar<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, inline, nestedClass, objectDeclaration */
