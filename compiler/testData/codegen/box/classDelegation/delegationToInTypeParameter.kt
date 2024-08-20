// ISSUE: KT-70186

interface Domain<in T : Any> {
    fun foo()
}

interface Bin<in T : Any, out V> : Domain<T>

class DomainBin<T : Comparable<T>, D : Domain<T>, out V>(val domain: D) : Bin<T, V>, Domain<T> by domain
class DomainBin2<T : Comparable<T>, D : Domain<T>, out V, G : D>(val domain: G) : Bin<T, V>, Domain<T> by domain

class DomainBin3<D : Domain<Int>>(val a: D) : Domain<Int> by a
class DomainBin4<D: Any>(val a: Domain<D>) : Domain<D> by a
class DomainBin5<D: Number>(val a: Domain<D>) : Domain<D> by a

class DomainBin6<D : Domain<Int>>(val a: D) : Domain<Int> by a
class DomainBin7<D : Domain<D>>(val a: D) : Domain<D> by a
class DomainBin8<A: Any, D : Domain<A>>(val a: D) : Domain<A> by a
class DomainBin9<A: Any, D : Domain<Domain<A>>>(val a: D) : Domain<Domain<A>> by a

class DomainBin10<D : Domain<D>, in A: D>(val a: D) : Domain<A> by a
class DomainBin11<A: Domain<*>, D : Domain<A>>(val a: D) : Domain<A> by a

fun box() = "OK"