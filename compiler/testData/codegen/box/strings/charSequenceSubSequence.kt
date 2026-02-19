// WITH_STDLIB

import kotlin.test.*

// CHECK_CALLED_IN_SCOPE: function=substring scope=getStringSubSequence
// CHECK_NOT_CALLED_IN_SCOPE: function=charSequenceSubSequence scope=getStringSubSequence
fun getStringSubSequence(s: String, start: Int, end: Int): CharSequence = s.subSequence(start, end)

// CHECK_CALLED_IN_SCOPE: function=charSequenceSubSequence scope=getCharSequenceSubSequence
// CHECK_NOT_CALLED_IN_SCOPE: function=substring scope=getCharSequenceSubSequence
fun getCharSequenceSubSequence(s: CharSequence, start: Int, end: Int): CharSequence = s.subSequence(start, end)

private class MyCharSequence(val s: String) : CharSequence by s

fun box(): String {
    assertEquals("el", getStringSubSequence("Hello world", 1, 3))
    assertEquals("el", getCharSequenceSubSequence("Hello world", 1, 3))
    assertEquals("el", getCharSequenceSubSequence(MyCharSequence("Hello world"), 1, 3))

    return "OK"
}