import kotlin.test.*

import kotlin.comparisons.*

fun box() {
    val sequence = sequenceOf(1, 2, 3, 4)
    sequence.toList()
    sequence.toList()

    val oneTime = sequence.constrainOnce()
    oneTime.toList()
    assertTrue("should fail with IllegalStateException") {
        assertFails {
            oneTime.toList()
        } is IllegalStateException
    }

}
