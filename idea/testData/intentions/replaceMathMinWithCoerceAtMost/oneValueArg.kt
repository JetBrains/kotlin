// WITH_RUNTIME
// IS_APPLICABLE: false
// ERROR: None of the following functions can be called with the arguments supplied: <br>public open fun min(a: Double, b: Double): Double defined in java.lang.Math<br>public open fun min(a: Float, b: Float): Float defined in java.lang.Math<br>public open fun min(a: Int, b: Int): Int defined in java.lang.Math<br>public open fun min(a: Long, b: Long): Long defined in java.lang.Math

import java.lang.Math.min

fun foo() {
    min(1.1)<caret>
}
