// WITH_RUNTIME

import kotlin.test.assertEquals

import java.util.Random

fun box() : String {
    val x: Int = Random().nextInt(5)

    val str = when (x) {
        is 0 -> "zero"
        is 1 -> "one"
        is 2 -> "two"
        is _ -> "many"
    }

    when (x) {
        0 -> assertEquals(str, "zero")
        1 -> assertEquals(str, "one")
        2 -> assertEquals(str, "two")
        else -> assertEquals(str, "many")
    }

    return "OK"
}