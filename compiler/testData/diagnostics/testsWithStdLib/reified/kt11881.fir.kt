class Inv<T>

inline operator fun <reified T> Inv<T>.invoke() {}

operator fun <K> Inv<K>.get(i: Int): Inv<K> = this

fun <K> test(a: Inv<K>) {
    a[1]()
}