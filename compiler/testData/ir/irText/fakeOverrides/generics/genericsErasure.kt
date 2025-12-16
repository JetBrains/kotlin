// WITH_STDLIB
// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// Disable K1 since it reports: CONFLICTING_OVERLOADS: Conflicting overloads: public final fun <A, B> foo4(a: A): Unit defined in Foo, public final fun <B> foo4(a: B): Unit defined in Foo
// IGNORE_BACKEND_K1: ANY

class Foo {
    @JvmName("a1") fun <A> foo1(a: A) where A : Number = Unit
    @JvmName("b1") fun foo1(a: Number) = Unit

    // Different members from the K1 reflection perspective (I think it's a bug). Can override each other
    @Suppress("CONFLICTING_OVERLOADS") fun <A> foo2(a: A) where A : java.io.Serializable, A : CharSequence = Unit
    @Suppress("CONFLICTING_OVERLOADS") fun <A> foo2(a: A) where A : CharSequence, A : java.io.Serializable = Unit

    // Different members from the K1 reflection perspective. Can't override each other
    @Suppress("CONFLICTING_OVERLOADS") @JvmName("a3") fun <A, B> foo3(a: A) = Unit
    @Suppress("CONFLICTING_OVERLOADS") @JvmName("b3") fun <A, B> foo3(a: B) = Unit

    // Different members from the K1 reflect perspective
    @JvmName("a4") fun <A, B> foo4(a: A) = Unit
    @JvmName("b4") fun <B> foo4(a: B) = Unit
}
