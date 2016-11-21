platform fun <T> f1(): T

platform fun <T> f2(t: T)

platform fun <K : Enum<K>, V : MutableList<out K>> f3(v: Map<V, Enum<in K>>, w: Comparable<*>)

platform fun <T: Comparable<T>> Array<out T>.sort(): Unit

platform class C1<A>
platform class C2<B : C2<B>>
platform class C3<D, E : MutableList<in D>>

platform abstract class AbstractList<F> : MutableList<F>
