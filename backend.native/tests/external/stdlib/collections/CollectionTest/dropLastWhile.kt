import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    val coll = listOf("Foo", "bare", "abc")
    assertEquals(coll, coll.dropLastWhile { false })
    assertEquals(listOf<String>(), coll.dropLastWhile { true })
    assertEquals(listOf("Foo", "bare"), coll.dropLastWhile { it.length < 4 })
    assertEquals(listOf("Foo"), coll.dropLastWhile { it.all { it in 'a'..'z' } })
}
