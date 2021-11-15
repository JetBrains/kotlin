// WITH_STDLIB
import kotlin.test.assertEquals

fun test() {
    var i = 0
    val a = ubyteArrayOf(3u, 2u, 1u)
    a.forEach { e -> assertEquals(e, a[i++]) }
}

fun box(): String {
    test()
    return "OK"
}
