// LANGUAGE: +ContextReceivers
// DIAGNOSTICS: -NOTHING_TO_INLINE -CONTEXT_RECEIVERS_DEPRECATED -CONTEXT_CLASS_OR_CONSTRUCTOR -CAST_NEVER_SUCCEEDS
// FIR_IDENTICAL
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT

private class A {
    internal class Nested
}

context(Int)
private class WithContext

private open class Generic<T>

private fun makeA(): A = A()

private fun makeNested(): A.Nested = A.Nested()

private fun makeWithContext(): WithContext = with(42) { WithContext() }

private inline fun privateInline(): Any = <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>makeA()<!>

private fun makeEffectivelyPrivateLocal() = object {
    public inner class Inner()
}.Inner()

private fun makeLocal() = object {}

public fun publicMakeLocal() = object {}

internal inline fun internalInline() {
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>val a = <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>makeA()<!><!>
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>a<!>.toString()
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>makeNested()<!>
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>makeLocal()<!>
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>makeEffectivelyPrivateLocal()<!>
    publicMakeLocal()
    privateInline()
    class Local : <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>Generic<A>()<!> {}
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>val withContext = <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>makeWithContext()<!><!>
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>withContext<!>.toString()
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>null as A<!>
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>null as A.Nested<!>
    <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>A::class<!>
}

internal inline fun referencePrivateInsideAnonymousObject() {
    object {
        private fun foo() {
            <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>val a = <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>makeA()<!><!>
            <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>a<!>.toString()
            <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>makeNested()<!>
            <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>makeLocal()<!>
            publicMakeLocal()
            <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>makeEffectivelyPrivateLocal()<!>
            privateInline()
            class Local : <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>Generic<A>()<!> {}
            <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>null as A<!>
            <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>null as A.Nested<!>
            <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>A::class<!>
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
            class Local : Generic<A>() {}
            null as A
            null as A.Nested
            A::class
        }
    }.foo()
}

private class PrivateOuter {
    private class PrivateNested {}

    internal inline fun usePrivateNested() {
        val a: PrivateNested? = null
    }
}
