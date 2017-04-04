import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    // lets use a mutable variable of readonly list
    val data = sequenceOf("cheese", "foo", "beer", "cheese", "wine")
    var l = data
    l -= "cheese"
    assertEquals(listOf("foo", "beer", "cheese", "wine"), l.toList())
    l = data
    l -= listOf("cheese", "beer")
    assertEquals(listOf("foo", "wine"), l.toList())
    l -= arrayOf("wine", "bar")
    assertEquals(listOf("foo"), l.toList())
}
