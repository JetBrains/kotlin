import kotlin.test.*

fun box() {
    expect(listOf("a"), { arrayOf("a", null).filterNotNull() })
}
