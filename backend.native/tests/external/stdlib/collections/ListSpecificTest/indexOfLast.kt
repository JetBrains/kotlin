import kotlin.test.*

val data = listOf("foo", "bar")
val empty = listOf<String>()
fun box() {
    expect(-1) { data.indexOfLast { it.contains("p") } }
    expect(1) { data.indexOfLast { it.length == 3 } }
    expect(-1) { empty.indexOfLast { it.startsWith('f') } }
}
