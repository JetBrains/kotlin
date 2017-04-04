import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    // lets use a mutable variable of readonly list
    val data: List<String> = listOf("cheese", "foo", "beer", "cheese", "wine")
    var l = data
    l -= "cheese"
    assertEquals(listOf("foo", "beer", "cheese", "wine"), l)
    l = data
    l -= listOf("cheese", "beer")
    assertEquals(listOf("foo", "wine"), l)
    l -= arrayOf("wine", "bar")
    assertEquals(listOf("foo"), l)

    val ml = arrayListOf("cheese", "cheese", "foo", "beer", "cheese", "wine")
    ml -= "cheese"
    assertEquals(listOf("cheese", "foo", "beer", "cheese", "wine"), ml)
    ml -= listOf("cheese", "beer")
    assertEquals(listOf("foo", "wine"), ml)
    ml -= arrayOf("wine", "bar")
    assertEquals(listOf("foo"), ml)
}
