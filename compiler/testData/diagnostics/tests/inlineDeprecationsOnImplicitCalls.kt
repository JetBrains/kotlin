// DIAGNOSTICS: -NOTHING_TO_INLINE

@PublishedApi
internal class InternalClassPrivateConstructor private constructor() {
    companion object {
        internal inline operator fun invoke(): InternalClassPrivateConstructor = InternalClassPrivateConstructor()

        internal inline operator fun invoke(i: Int): InternalClassPrivateConstructor = <!DEPRECATED_IMPLICIT_NON_PUBLIC_API_ACCESS!>InternalClassPrivateConstructor<!>(i)
    }
}

private class PrivateClass public constructor() {

    operator fun invoke() = 42
}

open class OpenClass {

    protected operator fun invoke() = 42

    inline fun foo() {
        <!DEPRECATED_IMPLICIT_NON_PUBLIC_API_ACCESS!>this<!>()
    }
}

@PublishedApi
internal open class InternalClassProtectedConstructor protected constructor() {
    companion object {
        internal inline operator fun invoke(): InternalClassProtectedConstructor = InternalClassProtectedConstructor()
    }
}

inline fun publicInline() {
    <!DEPRECATED_IMPLICIT_NON_PUBLIC_API_ACCESS!>InternalClassPrivateConstructor<!>()
    InternalClassPrivateConstructor.<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>invoke<!>()
    <!DEPRECATED_IMPLICIT_NON_PUBLIC_API_ACCESS!>InternalClassProtectedConstructor<!>()
}

internal inline fun internalInline() {
    val pc = <!PRIVATE_CLASS_MEMBER_FROM_INLINE!>PrivateClass<!>()
    <!DEPRECATED_IMPLICIT_NON_PUBLIC_API_ACCESS!>pc<!>()
}
