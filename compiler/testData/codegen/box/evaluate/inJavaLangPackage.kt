// Can't be tested in JVM because frontend doesn't allow such code
// DONT_TARGET_EXACT_BACKEND: JVM_IR
// WITH_STDLIB

package java.lang

object Math {
    const val E: Double = kotlin.math.E
    const val PI: Double = kotlin.math.PI
    const val OK: String = "OK"
}

const val usageE = Math.E
const val usagePI = Math.PI

fun box(): String {
    return Math.OK
}
