// !CHECK_TYPE

package n

import java.util.*

fun test() {
    val foo = arrayList("").map { it -> it.length }.fold(0, { x, y -> Math.max(x, y) })
    checkSubtype<Int>(foo)
    checkSubtype<String>(<!TYPE_MISMATCH!>foo<!>)
}

//from library
fun <T> arrayList(vararg <!UNUSED_PARAMETER!>values<!>: T) : ArrayList<T> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun <T, R> Collection<T>.map(<!UNUSED_PARAMETER!>transform<!> : (T) -> R) : List<R> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

fun <T> Iterable<T>.fold(<!UNUSED_PARAMETER!>initial<!>: T, <!UNUSED_PARAMETER!>operation<!>: (T, T) -> T): T {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>