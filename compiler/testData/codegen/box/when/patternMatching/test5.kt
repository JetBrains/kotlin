// WITH_RUNTIME

import kotlin.test.assertEquals

import java.util.Random

fun box() : String {
    val x: Int = Random().nextInt(5)

    val str = when (x) {
        match 0 -> "zero"
        match 1 -> "one"
        match 2 -> "two"
        match _ -> "many"
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