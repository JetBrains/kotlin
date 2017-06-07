import kotlin.text.*
import kotlin.test.*

fun assertTrue(msg: String, value: Boolean) = assertTrue(value, msg)
fun assertFalse(msg: String, value: Boolean) = assertFalse(value, msg)

fun codePointToString(codePoint: Int): String {
    val charArray = Char.toChars(codePoint)
    return fromCharArray(charArray, 0, charArray.size)
}

fun box() {}

// TODO: Here is a performance problem: an execution of this test requires much more time than it in Kotlin/JVM.
fun box1() {
    // Regression for HARMONY-3145
    var p = Regex("(\\p{all})+")
    var res = true
    var cnt = 0
    var s: String
    for (i in 0..1114111) {
        if (i % 200000 == 0) {
            println(i)
        }
        s = codePointToString(i)
        // if (!s.matches(p.toString().toRegex())) { TODO: Uncomment when caching is done.
        if (!s.matches(p)) {
            cnt++
            res = false
        }
    }
    assertTrue(res)
    assertEquals(0, cnt)

    p = Regex("(\\P{all})+")
    res = true
    cnt = 0

    for (i in 0..1114111) {
        if (i % 200000 == 0) {
            println(i)
        }
        s = codePointToString(i)
        // if (!s.matches(p.toString().toRegex())) { TODO: Uncomment when caching is done.
        if (!s.matches(p)) {
            cnt++
            res = false
        }
    }

    assertFalse(res)
    assertEquals(0x110000, cnt)
}