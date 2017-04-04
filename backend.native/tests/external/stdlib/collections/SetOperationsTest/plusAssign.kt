import kotlin.test.*


fun box() {
    // lets use a mutable variable
    var set = setOf("a")
    val setOriginal = set
    set += "foo"
    set += listOf("beer", "a")
    set += arrayOf("cheese", "beer")
    set += sequenceOf("bar", "foo")
    assertEquals(setOf("a", "foo", "beer", "cheese", "bar"), set)
    assertTrue(set !== setOriginal)

    val mset = mutableSetOf("a")
    mset += "foo"
    mset += listOf("beer", "a")
    mset += arrayOf("cheese", "beer")
    mset += sequenceOf("bar", "foo")
    assertEquals(set, mset)
}
