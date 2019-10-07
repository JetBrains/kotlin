// !DIAGNOSTICS: -UNUSED_VARIABLE, -UNUSED_TYPEALIAS_PARAMETER, -CAST_NEVER_SUCCEEDS
// !LANGUAGE: +TrailingCommas

class Foo1<T1> {}

interface Foo2<T1,>

fun <T1, T2, T3>foo3() {}

typealias Foo4<T1,T2,T3,T4> = Int

class Foo5<T, K: T,>: Foo2<K,>

fun <T>foo () {
    val x1 = Foo1<Int,>()
    val x2: Foo2<Int,>? = null
    val x21: Foo2<Int,/**/>? = null
    val x3 = foo3<
            Int,
            String,
            Float,
            >()
    val x4: Foo4<Comparable<Int,>, Iterable<Comparable<Float,>,>, Double, T,
            >? = null as Foo4<Comparable<Int,>, Iterable<Comparable<Float,>,>, Double, T,>
    val x5: (Float,) -> Unit = {}
    val x6: Pair<(Float, Comparable<T,>,) -> Unit, (Float,) -> Unit,>? = null
    val x61: Pair<(Float, Comparable<T,/**/>,/**/) -> Unit, (Float,/**/) -> Unit,/**/>? = null
}
