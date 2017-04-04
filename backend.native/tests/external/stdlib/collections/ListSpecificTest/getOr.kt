import kotlin.test.*

val data = listOf("foo", "bar")
val empty = listOf<String>()

fun box() {
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
