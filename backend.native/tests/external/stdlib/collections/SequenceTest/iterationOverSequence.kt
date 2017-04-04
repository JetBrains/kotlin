import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    var s = ""
    for (i in sequenceOf(0, 1, 2, 3, 4, 5)) {
        s += i.toString()
    }
    assertEquals("012345", s)
}
