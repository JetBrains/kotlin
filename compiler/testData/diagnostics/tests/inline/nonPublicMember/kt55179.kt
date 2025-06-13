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
    <!PRIVATE_CLASS_MEMBER_FROM_INLINE!>Foo<!>()
    Foo.<!PRIVATE_CLASS_MEMBER_FROM_INLINE!>Companion<!>
    Foo.<!PRIVATE_CLASS_MEMBER_FROM_INLINE_WARNING!>buildFoo<!>()
    Foo.Companion.Nested.bar()
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, inline, nestedClass, objectDeclaration */
