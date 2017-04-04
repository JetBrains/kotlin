import kotlin.text.*

import kotlin.test.*


fun box() {
    val p = "\\d+".toRegex()
    val input = "123 456 789"

    assertFalse(input matches p)
    assertFalse(p matches input)

    assertTrue(p in input)

    val first = p.find(input)
    assertTrue(first != null); first!!
    assertEquals("123", first.value)

    val second1 = first.next()!!
    val second2 = first.next()!!

    assertEquals("456", second1.value)
    assertEquals(second1.value, second2.value)

    assertEquals("56", p.find(input, startIndex = 5)?.value)

    val last = second1.next()!!
    assertEquals("789", last.value)

    val noMatch = last.next()
    assertEquals(null, noMatch)
}
