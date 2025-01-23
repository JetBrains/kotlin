// WITH_STDLIB

import kotlin.test.*

fun getStringChar(s: String, index: Int): Char = s[index]

fun getCharSequenceChar(s: CharSequence, index: Int): Char = s[index]

private class MyCharSequence(val s: String) : CharSequence by s

fun box(): String {
    assertEquals('a', getStringChar("a", 0))
    assertEquals('a', getCharSequenceChar("a", 0))
    assertEquals('a', getCharSequenceChar(MyCharSequence("a"), 0))

    assertEquals(Char.MIN_VALUE, getStringChar("${Char.MIN_VALUE}", 0))
    assertEquals(Char.MIN_VALUE, getCharSequenceChar("${Char.MIN_VALUE}", 0))
    assertEquals(Char.MIN_VALUE, getCharSequenceChar(MyCharSequence("${Char.MIN_VALUE}"), 0))

    assertEquals(Char.MAX_VALUE, getStringChar("${Char.MAX_VALUE}", 0))
    assertEquals(Char.MAX_VALUE, getCharSequenceChar("${Char.MAX_VALUE}", 0))
    assertEquals(Char.MAX_VALUE, getCharSequenceChar(MyCharSequence("${Char.MAX_VALUE}"), 0))

    return "OK"
}