import kotlin.test.*

import kotlin.comparisons.*

fun onEach() {
    var count = 0
    val data = sequenceOf("foo", "bar")
    val newData = data.onEach { count += it.length }
    assertFalse(data === newData)
    assertEquals(0, count, "onEach should be executed lazily")

    data.forEach { }
    assertEquals(0, count, "onEach should be executed only when resulting sequence is iterated")

    val sum = newData.sumBy { it.length }
    assertEquals(sum, count)
}

fun box() {
    var count = 0
    val data = sequenceOf("foo", "bar")
    val newData = data.onEach { count += it.length }
    assertFalse(data === newData)
    assertEquals(0, count, "onEach should be executed lazily")

    data.forEach { }
    assertEquals(0, count, "onEach should be executed only when resulting sequence is iterated")

    val sum = newData.sumBy { it.length }
    assertEquals(sum, count)
}
