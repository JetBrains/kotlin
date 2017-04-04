import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    // lets use a mutable variable
    var seq = sequenceOf("a")
    seq += "foo"
    seq += listOf("beer")
    seq += arrayOf("cheese", "wine")
    seq += sequenceOf("bar", "foo")
    assertEquals(listOf("a", "foo", "beer", "cheese", "wine", "bar", "foo"), seq.toList())
}
