// !CHECK_TYPE

package n

import java.util.*
import checkSubtype

fun test() {
    val foo = arrayList("").map { it -> it.length }.fold(0, { x, y -> Math.max(x, y) })
    checkSubtype<Int>(foo)
    checkSubtype<String>(<!ARGUMENT_TYPE_MISMATCH!>foo<!>)
}

//from library
fun <T> arrayList(vararg values: T) : ArrayList<T> {}

fun <T, R> Collection<T>.map(transform : (T) -> R) : List<R> {}

fun <T> Iterable<T>.fold(initial: T, operation: (T, T) -> T): T {}
