import kotlin.text.*

import kotlin.test.*


fun box() {
    val regex = "a+b+?".toRegex()
    val input = StringBuilder("aaaabbbb")

    assertEquals("aaaab", regex.find(input)!!.value)
    assertEquals("aaaabbbb", regex.matchEntire(input)!!.value)
}
