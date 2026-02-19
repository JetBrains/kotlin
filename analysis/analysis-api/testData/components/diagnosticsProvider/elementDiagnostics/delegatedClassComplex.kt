// ISSUE: KT-77551
// WITH_STDLIB

abstract class Base<E>(classFilter: Filter<E>): Filter<E> by classFilter

interface Filter<E> {
    val itemClass: Class<E>
}

inline fun <reified T> filterByClass(): Filter<T> = object : Filter<T> {
    override val itemClass: Class<T>
        get() = clazz
}

interface Item

object Derived<caret>FromBase : Base<Item>(filterByClass()) {
    override fun convertValue(value: Item): String? = ""
}
