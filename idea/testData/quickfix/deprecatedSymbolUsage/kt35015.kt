// "Replace with 'kotlin.math.cos(x)'" "true"
// WITH_RUNTIME

package package1

import package1.Math.cos

object Math {
    @Deprecated("Replace", ReplaceWith("kotlin.math.cos(x)", "kotlin.math.cos"))
    fun cos(x: Double): Double = kotlin.math.cos(x)
}

val test = <caret>cos(kotlin.math.PI)