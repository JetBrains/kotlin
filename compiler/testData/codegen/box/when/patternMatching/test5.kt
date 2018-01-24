// WITH_RUNTIME

import kotlin.test.assertEquals

import java.util.Random

fun box() : String {
    val x: Int = Random().nextInt(5)

    val str = when (x) {
        0 -> "zero"
        1 -> "one"
        2 -> "two"
        is _ -> "many"
        else -> throw java.lang.IllegalStateException("Unexpected else")
    }

    when (x) {
        0 -> assertEquals(str, "zero")
        1 -> assertEquals(str, "one")
        2 -> assertEquals(str, "two")
        else -> assertEquals(str, "many")
    }

    return "OK"
}