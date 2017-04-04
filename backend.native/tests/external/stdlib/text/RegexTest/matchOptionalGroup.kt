import kotlin.text.*

import kotlin.test.*


fun box() {
    val pattern = "(hi)|(bye)".toRegex(RegexOption.IGNORE_CASE)

    pattern.find("Hi!")!!.let { m ->
        assertEquals(3, m.groups.size)
        assertEquals("Hi", m.groups[1]?.value)
        assertEquals(null, m.groups[2])

        assertEquals(listOf("Hi", "Hi", ""), m.groupValues)

        val (g1, g2) = m.destructured
        assertEquals("Hi", g1)
        assertEquals("", g2)
        assertEquals(listOf("Hi", ""), m.destructured.toList())
    }

    pattern.find("bye...")!!.let { m ->
        assertEquals(3, m.groups.size)
        assertEquals(null, m.groups[1])
        assertEquals("bye", m.groups[2]?.value)

        assertEquals(listOf("bye", "", "bye"), m.groupValues)

        val (g1, g2) = m.destructured
        assertEquals("", g1)
        assertEquals("bye", g2)
        assertEquals(listOf("", "bye"), m.destructured.toList())
    }
}
