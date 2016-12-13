header fun <T> f1(): T

header fun <T> f2(t: T)

header fun <K : Enum<K>, V : MutableList<out K>> f3(v: Map<V, Enum<in K>>, w: Comparable<*>)

header fun <T: Comparable<T>> Array<out T>.sort(): Unit

header class C1<A>
header class C2<B : C2<B>>
header class C3<D, E : MutableList<in D>>

header abstract class AbstractList<F> : MutableList<F>
