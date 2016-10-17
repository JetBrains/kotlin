// WITH_RUNTIME

import java.util.SortedMap

fun test() {
    val foo: SortedMap<String, String>? = null
    foo?.toSortedMap()<caret>
}