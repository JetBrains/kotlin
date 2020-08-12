import kotlin.contracts.*

infix fun <E, R> ((E) -> R).returnsForEachOf(collection: Collection<E>)

inline fun <E, R> Collection<E>.forEach(block: E.() -> R) {
    contract {
        block returnsForEachOf this@forEach
    }
    for (element in this) block(element)
}

fun test1(set: Set<Number>) {
    set.forEach { this as Int }
    set.first().dec() // OK
}

fun test2(list: List<Number>) {
    list.forEach { this as Int }
    list.first().dec() // OK
    list[0].dec() // OK
}

fun test3(list: MutableList<Number>) {
    list.forEach { this as Int }
    list.first().dec() // OK
    list[0].dec() // OK
}

fun test4(list: ArrayList<Number>) {
    list.forEach { this as Int }
    list.first().dec() // OK
    list[0].dec() // OK
}