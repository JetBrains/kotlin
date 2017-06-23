// WITH_RUNTIME
// IS_APPLICABLE: false
// ERROR: None of the following functions can be called with the arguments supplied: <br>public open fun max(a: Double, b: Double): Double defined in java.lang.Math<br>public open fun max(a: Float, b: Float): Float defined in java.lang.Math<br>public open fun max(a: Int, b: Int): Int defined in java.lang.Math<br>public open fun max(a: Long, b: Long): Long defined in java.lang.Math

import java.lang.Math.max

fun foo() {
    max(1.1, 1.2, 1.3)<caret>
}
