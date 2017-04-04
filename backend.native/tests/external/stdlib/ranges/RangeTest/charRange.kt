import kotlin.test.*


fun box() {
    val range = 'c'..'w'
    assertFalse('0' in range)
    assertFalse('b' in range)

    assertTrue('c' in range)
    assertTrue('d' in range)
    assertTrue('h' in range)
    assertTrue('m' in range)
    assertTrue('v' in range)
    assertTrue('w' in range)

    assertFalse('z' in range)
    assertFalse('\u1000' in range)

    assertFalse(range.isEmpty())

    assertTrue('v' in (range as ClosedRange<Char>))
    assertFalse((range as ClosedRange<Char>).isEmpty())

    val openRange = 'A' until 'Z'
    assertTrue('Y' in openRange)
    assertFalse('Z' in openRange)

    assertTrue(('A' until '\u0000').isEmpty())
}
