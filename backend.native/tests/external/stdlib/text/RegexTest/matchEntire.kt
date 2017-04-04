import kotlin.text.*

import kotlin.test.*


fun box() {
    val regex = "(\\d)(\\w)".toRegex()

    assertNull(regex.matchEntire("1a 2b"))
    assertNotNull(regex.matchEntire("3c")) { m ->
        assertEquals("3c", m.value)
        assertEquals(3, m.groups.size)
        assertEquals(listOf("3c", "3", "c"), m.groups.map { it!!.value })
    }
}
