package runtime.basic.initializers4

import kotlin.test.*

const val INT_MAX_POWER_OF_TWO: Int = Int.MAX_VALUE / 2 + 1
val DOUBLE = Double.MAX_VALUE - 1.0

@Test fun runTest() {
    println(INT_MAX_POWER_OF_TWO)
    println(DOUBLE > 0.0)
}
