import kotlin.test.*

// helper predicates available on both platforms
fun Char.isAsciiDigit() = this in '0'..'9'
fun Char.isAsciiLetter() = this in 'A'..'Z' || this in 'a'..'z'
fun Char.isAsciiUpperCase() = this in 'A'..'Z'

fun box() {
    val data = "a1b2c3"
    val pair = data.partition { it.isAsciiDigit() }
    assertEquals("123", pair.first, "pair.first")
    assertEquals("abc", pair.second, "pair.second")
}
