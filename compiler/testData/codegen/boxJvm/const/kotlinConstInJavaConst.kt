// LANGUAGE: +IntrinsicConstEvaluation
// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: JUses.java
public class JUses {
    public static final String FROM_KOTLIN = KConst.TRIMMED;
    public static final int SUM = KConst.BUMPED + 1;
    public static final byte MASKED = KConst.MASKED;
    public static final String CONCAT = KConst.TRIMMED + "!";
}

// FILE: box.kt
import kotlin.experimental.and

object KConst {
    const val TRIMMED = "  OK  ".trim()
    const val N = 41
    const val BUMPED = N.inc()
    const val MASK: Byte = 0x0F
    const val MASKED: Byte = MASK and 0x3C.toByte()
}

const val roundTrip: String = JUses.FROM_KOTLIN.trim()
const val roundTripUpper: String = JUses.CONCAT.uppercase()
const val roundTripInt: Int = JUses.SUM.inc()

fun box(): String {
    if (JUses.FROM_KOTLIN != "OK") return "Fail FROM_KOTLIN: ${JUses.FROM_KOTLIN}"
    if (JUses.SUM != 43) return "Fail SUM: ${JUses.SUM}"
    if (JUses.MASKED != 0x0C.toByte()) return "Fail MASKED: ${JUses.MASKED}"
    if (JUses.CONCAT != "OK!") return "Fail CONCAT: ${JUses.CONCAT}"
    if (roundTrip != "OK") return "Fail roundTrip: $roundTrip"
    if (roundTripUpper != "OK!") return "Fail roundTripUpper: $roundTripUpper"
    if (roundTripInt != 44) return "Fail roundTripInt: $roundTripInt"
    return "OK"
}
