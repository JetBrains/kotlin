fun <T> myRun(computable: () -> T): T = TODO()

interface Inv<W>
interface MyMap<K, V> {
    val k: K
    val v: V
}

val w: Inv<String> = TODO()

public fun <X, K> Inv<X>.associateBy1(keySelector: (X) -> K): MyMap<K, X> = TODO()

val x = myRun {
    w.associateBy1 { f -> f.length }
}

fun foo(m: MyMap<Int, String>) {}

fun main() {
    foo(x)
}
