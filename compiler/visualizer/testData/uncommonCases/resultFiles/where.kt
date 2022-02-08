// FIR_IGNORE
//                            collections/List<T>     collections/List<String>
//                            │                       │
fun <T> copyWhenGreater(list: List<T>, threshold: T): List<String>
        where T : CharSequence,
              T : Comparable<T> {
//         copyWhenGreater.list: collections/List<T>
//         │    fun <T> collections/Iterable<T>.filter<T>((T) -> Boolean): collections/List<T>
//         │    │        copyWhenGreater.<anonymous>.it: T
//         │    │        │  fun (Comparable<T>).compareTo(T): Int
//         │    │        │  │ copyWhenGreater.threshold: T
//         │    │        │  │ │           fun <T, R> collections/Iterable<T>.map<T, String>((T) -> R): collections/List<R>
//         │    │        │  │ │           │     copyWhenGreater.<anonymous>.it: T
//         │    │        │  │ │           │     │  fun (Any).toString(): String
//         │    │        │  │ │           │     │  │
    return list.filter { it > threshold }.map { it.toString() }
}

fun main() {
//      collections/List<String>
//      │      fun <T> collections/listOf<String>(vararg T): collections/List<T>
//      │      │
    val list = listOf("1", "2", "3")
//      collections/List<String>
//      │      fun <T : CharSequence> copyWhenGreater<String>(collections/List<T>, T): collections/List<String> where T : Comparable<T>
//      │      │               val main.list: collections/List<String>
//      │      │               │
    val copy = copyWhenGreater(list, "2")
}
