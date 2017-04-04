import kotlin.test.*

// helper predicates available on both platforms
fun Char.isAsciiDigit() = this in '0'..'9'
fun Char.isAsciiLetter() = this in 'A'..'Z' || this in 'a'..'z'
fun Char.isAsciiUpperCase() = this in 'A'..'Z'

fun box() {
    val data = "ab1cd2"
    assertEquals("ab", data.takeWhile { it.isAsciiLetter() })
    assertEquals("", data.takeWhile { false })
    assertEquals("ab1cd2", data.takeWhile { true })
}
