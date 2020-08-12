import kotlin.contracts.*

infix fun <E, R> ((E) -> R).returnsForEachOf(collection: Collection<E>)

inline fun <E, R> Collection<E>.forEach(block: (E) -> R) {
    contract {
        block returnsForEachOf this@forEach
    }
    for (element in this) block(element)
}

fun test1(set: Set<String?>) {
    set.forEach { it!! }
    set.first().length // OK
}

fun test2(list: List<String?>) {
    list.forEach { it!! }
    list.first().length // OK
    list[0].length // OK
}

fun test3(list: MutableList<String?>) {
    list.forEach { it!! }
    list.first().length // OK
    list[0].length // OK
}

fun test4(list: ArrayList<String?>) {
    list.forEach { it!! }
    list.first().length // OK
    list[0].length // OK
}