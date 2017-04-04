import kotlin.test.*


fun box() {
    StringBuilder().let { sb ->
        assertEquals(0, sb.length)
        assertEquals("", sb.toString())
    }

    StringBuilder(16).let { sb ->
        assertEquals(0, sb.length)
        assertEquals("", sb.toString())
    }

    StringBuilder("content").let { sb ->
        assertEquals(7, sb.length)
        assertEquals("content", sb.toString())
    }

    StringBuilder(StringBuilder("content")).let { sb ->
        assertEquals(7, sb.length)
        assertEquals("content", sb.toString())
    }
}
