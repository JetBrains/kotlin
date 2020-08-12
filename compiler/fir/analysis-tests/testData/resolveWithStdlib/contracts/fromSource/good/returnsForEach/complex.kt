import kotlin.contracts.*

infix fun <E, R> ((E) -> R).returnsForEachOf(collection: Collection<E>)

inline fun <E, R> Collection<E>.forEach(block: E.() -> R) {
    contract {
        block returnsForEachOf this@forEach
    }
    for (element in this) block(element)
}

fun requireInt(value: Any?): Int? {
    contract {
        returns() implies (value is Int?)
    }
    return value as Int?
}

fun test1(set: Set<Any?>) {
    set.forEach {
        this!!.hashCode()
        requireInt(this)
    }
    set.first().dec() // OK
}

fun test2(list: List<Any?>) {
    list.forEach {
        this!!.hashCode()
        requireInt(this)
    }
    list.first().dec() // OK
    list[0].dec() // OK
}

fun test3(list: MutableList<Any?>) {
    list.forEach {
        this!!.hashCode()
        requireInt(this)
    }
    list.first().dec() // OK
    list[0].dec() // OK
}

fun test4(list: ArrayList<Number?>) {
    list.forEach {
        this!!.hashCode()
        requireInt(this)
    }
    list.first().dec() // OK
    list[0].dec() // OK
}