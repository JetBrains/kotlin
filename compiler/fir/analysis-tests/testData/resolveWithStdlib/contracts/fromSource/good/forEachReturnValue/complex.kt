import kotlin.contracts.*

fun returnValue(): Any? = null
operator fun <E> ((E) -> Boolean).not(): ((E) -> Boolean) = { it -> !this@not(it) }
infix fun <E> ((E) -> Boolean).forEachOf(returnValue: Any?)

inline fun <T> List<T>.filter(predicate: (T) -> Boolean): List<T> {
    contract {
        predicate forEachOf returnValue()
    }
    return this
}

inline fun <T> List<T>.filterNot(predicate: (T) -> Boolean): List<T> {
    contract {
        !predicate forEachOf returnValue()
    }
    return this
}

fun test1(list: List<Any?>) {
    val filtered = list.filter {
        if (true) {
            if (true) {
                it is String && it[0] == 'a'
            } else {
                it is CharSequence && it is String
            }
        } else {
            it != null && it is String?
        }
    }
    filtered.first().length // OK
    filtered[0].length // OK
}

fun test2(list: List<Any?>) {
    val filtered = list.filterNot {
        if (true) {
            if (true) {
                it !is String || it[0] != 'a'
            } else {
                it !is CharSequence || it !is String
            }
        } else {
            it == null || it !is String?
        }
    }
    filtered.first().length // OK
    filtered[0].length // OK
}