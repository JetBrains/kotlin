import kotlin.test.*

val data = listOf("foo", "bar")
val empty = listOf<String>()

fun box() {
    assertEquals("[foo, bar]", data.toString())
}

fun tail() {
    val data = listOf("foo", "bar", "whatnot")
    val actual = data.drop(1)
    assertEquals(listOf("bar", "whatnot"), actual)
}

fun slice() {
    val list = listOf('A', 'B', 'C', 'D')
    // ABCD
    // 0123
    assertEquals(listOf('B', 'C', 'D'), list.slice(1..3))
    assertEquals(listOf('D', 'C', 'B'), list.slice(3 downTo 1))

    val iter = listOf(2, 0, 3)
    assertEquals(listOf('C', 'A', 'D'), list.slice(iter))
}

fun getOr() {
    expect("foo") { data.get(0) }
    expect("bar") { data.get(1) }
    assertFails { data.get(2) }
    assertFails { data.get(-1) }
    assertFails { empty.get(0) }

    expect("foo") { data.getOrElse(0, { "" }) }
    expect("zoo") { data.getOrElse(-1, { "zoo" }) }
    expect("zoo") { data.getOrElse(2, { "zoo" }) }
    expect("zoo") { empty.getOrElse(0) { "zoo" } }

    expect(null) { empty.getOrNull(0) }

}
