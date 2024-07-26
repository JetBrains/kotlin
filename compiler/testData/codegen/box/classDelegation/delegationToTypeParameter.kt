// ISSUE: KT-70186

interface Domain<in T : Any> {
    fun foo()
}

interface Bin<in T : Any, out V> : Domain<T>

class DomainBin<T : Comparable<T>, D : Domain<T>, out V>(val domain: D) : Bin<T, V>, Domain<T> by domain
class DomainBin2<T : Comparable<T>, D : Domain<T>, out V, G : D>(val domain: G) : Bin<T, V>, Domain<T> by domain

fun box() = "OK"