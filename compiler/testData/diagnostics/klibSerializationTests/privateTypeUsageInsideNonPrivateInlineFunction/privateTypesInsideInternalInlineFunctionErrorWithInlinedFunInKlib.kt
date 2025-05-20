// LANGUAGE: +IrInlinerBeforeKlibSerialization
// LANGUAGE: +ForbidExposureOfPrivateTypesInNonPrivateInlineFunctionsInKlibs +ContextReceivers
// IGNORE_FIR_DIAGNOSTICS
// DIAGNOSTICS: -NOTHING_TO_INLINE -CONTEXT_RECEIVERS_DEPRECATED -CONTEXT_CLASS_OR_CONSTRUCTOR -CAST_NEVER_SUCCEEDS
// FIR_IDENTICAL
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT

private class A {
    internal class Nested
}

private interface I

private object O : I

private annotation class AC()

private enum class EC {
    EE
}

context(Int)
private class WithContext

private open class Generic<T>

private fun makeA(): A = A()

private fun makeNested(): A.Nested = A.Nested()

private fun makeWithContext(): WithContext = with(42) { WithContext() }

private inline fun privateInline(): Any = makeA()

private inline fun privateInlineO(): Any = O

private inline fun privateInlineI(): I = O

private inline fun privateInlineAC(): Any = AC()

private inline fun privateInlineEC(): Any = EC.EE

private fun makeEffectivelyPrivateLocal() = object {
    public inner class Inner()
}.Inner()

private fun makeLocal() = object {}

public fun publicMakeLocal() = object {}

internal inline fun internalInline() {
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>val a = <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!><!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING!>makeA<!>()<!><!>
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>a<!>.toString()
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!><!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING!>makeNested<!>()<!>
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!><!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING!>makeLocal<!>()<!>
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!><!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING!>makeEffectivelyPrivateLocal<!>()<!>
    publicMakeLocal()
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_CASCADING_ERROR!>privateInline()<!>
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_CASCADING_ERROR!>privateInlineO()<!>
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_CASCADING_ERROR!><!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING!>privateInlineI<!>()<!>
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_CASCADING_ERROR!>privateInlineAC()<!>
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_CASCADING_ERROR!>privateInlineEC()<!>
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>class Local : <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING, PRIVATE_CLASS_MEMBER_FROM_INLINE!>Generic<<!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>A<!>><!>() {}<!>
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>val withContext = <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!><!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING!>makeWithContext<!>()<!><!>
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>withContext<!>.toString()
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>null as <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>A<!><!>
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>null as <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>A.Nested<!><!>
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!><!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>A<!>::class<!>
}

internal inline fun referencePrivateInsideAnonymousObject() {
    object {
        private fun foo() {
            <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>val a = <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!><!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING!>makeA<!>()<!><!>
            <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>a<!>.toString()
            <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!><!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING!>makeNested<!>()<!>
            <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!><!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING!>makeLocal<!>()<!>
            publicMakeLocal()
            <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!><!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING!>makeEffectivelyPrivateLocal<!>()<!>
            <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_CASCADING_ERROR!>privateInline()<!>
            <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>class Local : <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING, PRIVATE_CLASS_MEMBER_FROM_INLINE!>Generic<<!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>A<!>><!>() {}<!>
            <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>null as <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>A<!><!>
            <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>null as <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>A.Nested<!><!>
            <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!><!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>A<!>::class<!>
        }
    }
}

private class B {
    internal inline fun foo() {
        makeA()
        makeNested()
        makeLocal()
        publicMakeLocal()
        makeEffectivelyPrivateLocal()
        privateInline()
        class Local : Generic<A>() {}
        null as A
        null as A.Nested
        A::class
    }
}

internal class C {
    private class Nested1 {
        internal class Nested2 {
            internal inline fun foo() {
                val a = makeA()
                a.toString()
                makeNested()
                makeLocal()
                publicMakeLocal()
                makeEffectivelyPrivateLocal()
                privateInline()
                class Local : Generic<A>() {}
                null as A
                null as A.Nested
                A::class
            }
        }
    }
}

internal inline fun withAnonymousObject() {
    object {
        private inner class Inner {}
        fun foo() { Inner() }
    }.foo()
}

internal fun inlineInsideAnonymousObject() {
    object {
        private inner class Inner {}
        internal inline fun foo() {
            val a = makeA()
            a.toString()
            makeNested()
            makeLocal()
            publicMakeLocal()
            makeEffectivelyPrivateLocal()
            privateInline()
            Inner()
            class Local : <!PRIVATE_CLASS_MEMBER_FROM_INLINE!>Generic<A><!>() {}
            null as A
            null as A.Nested
            A::class
        }
    }.foo()
}

private class PrivateOuter {
    private class PrivateNested {}

    internal inline fun usePrivateNested() {
        val a: <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>PrivateNested<!>? = null
    }
}
