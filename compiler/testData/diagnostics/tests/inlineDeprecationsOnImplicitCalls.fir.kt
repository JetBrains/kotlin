// DIAGNOSTICS: -NOTHING_TO_INLINE

@PublishedApi
internal class InternalClassPrivateConstructor private constructor() {
    companion object {
        internal inline operator fun invoke(): InternalClassPrivateConstructor = InternalClassPrivateConstructor()

        internal inline operator fun invoke(i: Int): InternalClassPrivateConstructor = <!RECURSION_IN_INLINE!>InternalClassPrivateConstructor<!>(i)
    }
}

private class PrivateClass public constructor() {

    operator fun invoke() = 42
}

open class OpenClass {

    protected operator fun invoke() = 42

    inline fun foo() {
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>this<!>()
    }
}

@PublishedApi
internal open class InternalClassProtectedConstructor protected constructor() {
    companion object {
        internal inline operator fun invoke(): InternalClassProtectedConstructor = InternalClassProtectedConstructor()
    }
}

inline fun publicInline() {
    <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>InternalClassPrivateConstructor<!>()
    InternalClassPrivateConstructor.<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>invoke<!>()
    <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>InternalClassProtectedConstructor<!>()
}

internal inline fun internalInline() {
    val pc = <!PRIVATE_CLASS_MEMBER_FROM_INLINE!>PrivateClass<!>()
    <!PRIVATE_CLASS_MEMBER_FROM_INLINE!>pc<!>()
}
