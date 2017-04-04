import kotlin.text.*

import kotlin.test.*


fun box() {
    val input = "1a 2b 3c"
    val pattern = "(\\d)(\\w)".toRegex()

    val matches = pattern.findAll(input).toList()
    assertTrue(matches.all { it.groups.size == 3 })

    matches[0].let { m ->
        assertEquals("1a", m.groups[0]?.value)
        assertEquals("1", m.groups[1]?.value)
        assertEquals("a", m.groups[2]?.value)

        assertEquals(listOf("1a", "1", "a"), m.groupValues)

        val (g1, g2) = m.destructured
        assertEquals("1", g1)
        assertEquals("a", g2)
        assertEquals(listOf("1", "a"), m.destructured.toList())
    }

    matches[1].let { m ->
        assertEquals("2b", m.groups[0]?.value)
        assertEquals("2", m.groups[1]?.value)
        assertEquals("b", m.groups[2]?.value)

        assertEquals(listOf("2b", "2", "b"), m.groupValues)

        val (g1, g2) = m.destructured
        assertEquals("2", g1)
        assertEquals("b", g2)
        assertEquals(listOf("2", "b"), m.destructured.toList())
    }
}
