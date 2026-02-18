// FIR_IDENTICAL
// WITH_STDLIB
// TARGET_BACKEND: JVM

// The test primarily tests reflect dumps (k1 vs new reflect), we don't need kt dumps
// SKIP_KT_DUMP

// Disable K1 since it reports: CONFLICTING_OVERLOADS: Conflicting overloads: public final fun <T : I1#1 (type parameter of A.foo), I1> foo(): Unit defined in A, public final fun <T : I1> foo(): Unit defined in A
// IGNORE_BACKEND_K1: ANY

class A {
    @JvmName("bar1") fun <T> bar(t: T) where T : I1, T : I2 = Unit
    @JvmName("bar2") fun <T> bar(t: T) where T : I1, T : I3 = Unit

    @JvmName("foo1") fun <T : I1, I1> foo() = Unit
    @JvmName("foo2") fun <T : I1> foo() = Unit

    fun <T : Number> baz(t: T) = Unit
    fun <T : CharSequence> baz(t: T) = Unit
}

open class Base<R> {
    fun <T : I1> foo(t: T) = Unit
    fun <T : R> foo(t: T) = Unit
}

class B : Base<I1>()

interface I1
interface I2
interface I3
