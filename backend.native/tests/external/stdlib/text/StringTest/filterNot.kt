import kotlin.test.*

// helper predicates available on both platforms
fun Char.isAsciiDigit() = this in '0'..'9'
fun Char.isAsciiLetter() = this in 'A'..'Z' || this in 'a'..'z'
fun Char.isAsciiUpperCase() = this in 'A'..'Z'

fun box() {
    assertEquals("acdca", ("abcdcba").filterNot { it.equals('b') })
    assertEquals("abcd", ("a1b2c3d4").filterNot { it.isAsciiDigit() })
}
