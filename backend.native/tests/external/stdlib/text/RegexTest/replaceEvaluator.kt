import kotlin.text.*

import kotlin.test.*


fun box() {
    val input = "/12/456/7890/"
    val pattern = "\\d+".toRegex()
    assertEquals("/2/3/4/", pattern.replace(input, { it.value.length.toString() } ))
}
