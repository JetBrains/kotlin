// WITH_STDLIB
import kotlin.test.*

fun box(): String {
    val s = "world"
    val i = 1
    val res1 = "Hello $s $i ${2 * i}"
    if (res1 != "Hello world 1 2") return "FAIL 1: $res1"

    val a = "a"
    val res2 = "Hello, $a"
    if (res2 != "Hello, a") return "FAIL 2: $res2"

    return "OK"
}
