// !DIAGNOSTICS: -UNUSED_VARIABLE, -UNUSED_TYPEALIAS_PARAMETER, -CAST_NEVER_SUCCEEDS

class Foo1<T1> {}

interface Foo2<T1<!UNSUPPORTED_FEATURE!>,<!>>

fun <T1, T2, T3>foo3() {}

typealias Foo4<T1,T2,T3,T4> = Int

class Foo5<T, K: T<!UNSUPPORTED_FEATURE!>,<!>>: Foo2<K<!UNSUPPORTED_FEATURE!>,<!>>

fun <T> foo() {
    val x1 = Foo1<Int,>()
    val x2: Foo2<Int<!UNSUPPORTED_FEATURE!>,<!>>? = null
    val x21: Foo2<Int<!UNSUPPORTED_FEATURE!>,<!>/**/>? = null
    val x3 = foo3<
            Int,
            String,
            Float,
            >()
    val x4: Foo4<Comparable<Int<!UNSUPPORTED_FEATURE!>,<!>>, Iterable<Comparable<Float<!UNSUPPORTED_FEATURE!>,<!>><!UNSUPPORTED_FEATURE!>,<!>>, Double, T<!UNSUPPORTED_FEATURE!>,<!>
            >? = null as Foo4<Comparable<Int<!UNSUPPORTED_FEATURE!>,<!>>, Iterable<Comparable<Float<!UNSUPPORTED_FEATURE!>,<!>><!UNSUPPORTED_FEATURE!>,<!>>, Double, T<!UNSUPPORTED_FEATURE!>,<!>>
    val x5: (Float<!UNSUPPORTED_FEATURE!>,<!>) -> Unit = {}
    val x6: Pair<(Float, Comparable<T<!UNSUPPORTED_FEATURE!>,<!>><!UNSUPPORTED_FEATURE!>,<!>) -> Unit, (Float<!UNSUPPORTED_FEATURE!>,<!>) -> Unit<!UNSUPPORTED_FEATURE!>,<!>>? = null
    val x61: Pair<(Float, Comparable<T<!UNSUPPORTED_FEATURE!>,<!>/**/><!UNSUPPORTED_FEATURE!>,<!>/**/) -> Unit, (Float<!UNSUPPORTED_FEATURE!>,<!>/**/) -> Unit<!UNSUPPORTED_FEATURE!>,<!>/**/>? = null
}

