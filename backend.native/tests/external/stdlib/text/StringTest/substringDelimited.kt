import kotlin.test.*



fun box() {
    val s = "-1,22,3+"
    // chars
    assertEquals("22,3+", s.substringAfter(','))
    assertEquals("3+", s.substringAfterLast(','))
    assertEquals("-1", s.substringBefore(','))
    assertEquals("-1,22", s.substringBeforeLast(','))

    // strings
    assertEquals("22,3+", s.substringAfter(","))
    assertEquals("3+", s.substringAfterLast(","))
    assertEquals("-1", s.substringBefore(","))
    assertEquals("-1,22", s.substringBeforeLast(","))

    // non-existing delimiter
    assertEquals("", s.substringAfter("+"))
    assertEquals("", s.substringBefore("-"))
    assertEquals(s, s.substringBefore("="))
    assertEquals(s, s.substringAfter("="))
    assertEquals("xxx", s.substringBefore("=", "xxx"))
    assertEquals("xxx", s.substringAfter("=", "xxx"))

}
