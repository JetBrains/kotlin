// WITH_RUNTIME
// IS_APPLICABLE: false
// ERROR: None of the following functions can be called with the arguments supplied: <br>public open fun max(p0: Double, p1: Double): Double defined in java.lang.Math<br>public open fun max(p0: Float, p1: Float): Float defined in java.lang.Math<br>public open fun max(p0: Int, p1: Int): Int defined in java.lang.Math<br>public open fun max(p0: Long, p1: Long): Long defined in java.lang.Math

import java.lang.Math.max

fun foo() {
    max(1.1)<caret>
}