import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    // lets use a mutable variable of readonly list
    var l: List<String> = listOf("cheese")
    val lOriginal = l
    l += "foo"
    l += listOf("beer")
    l += arrayOf("cheese", "wine")
    l += sequenceOf("bar", "foo")
    assertEquals(listOf("cheese", "foo", "beer", "cheese", "wine", "bar", "foo"), l)
    assertTrue(l !== lOriginal)

    val ml = arrayListOf("cheese")
    ml += "foo"
    ml += listOf("beer")
    ml += arrayOf("cheese", "wine")
    ml += sequenceOf("bar", "foo")
    assertEquals(l, ml)
}
