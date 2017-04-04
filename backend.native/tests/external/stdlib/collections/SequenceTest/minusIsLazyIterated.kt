import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    val seq = sequenceOf("foo", "bar")
    val list = arrayListOf<String>()
    val result = seq - list

    list += "foo"
    assertEquals(listOf("bar"), result.toList())
    list += "bar"
    assertEquals(emptyList<String>(), result.toList())
}
