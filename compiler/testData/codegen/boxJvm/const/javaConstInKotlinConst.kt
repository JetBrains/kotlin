// LANGUAGE: +IntrinsicConstEvaluation
// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: J.java
public class J {
    public static final String PADDED = "  OK  ";
    public static final byte MASK = 0x0F;
    public static final int N = 41;
    public static final char C = 'a';
}

// FILE: box.kt
import kotlin.experimental.and
import kotlin.experimental.inv

const val trimmed: String = J.PADDED.trim()
const val upper: String = J.PADDED.trim().uppercase()
const val masked: Byte = J.MASK and 0x3C.toByte()
const val inverted: Byte = J.MASK.inv()
const val bumped: Int = J.N.inc()
const val fromCode: Char = Char(J.N + 24)
const val fromJavaChar: String = J.C.toString().uppercase()

fun box(): String {
    if (trimmed != "OK") return "Fail trimmed: $trimmed"
    if (upper != "OK") return "Fail upper: $upper"
    if (masked != 0x0C.toByte()) return "Fail masked: $masked"
    if (inverted != (-16).toByte()) return "Fail inverted: $inverted"
    if (bumped != 42) return "Fail bumped: $bumped"
    if (fromCode != 'A') return "Fail fromCode: $fromCode"
    if (fromJavaChar != "A") return "Fail fromJavaChar: $fromJavaChar"
    return "OK"
}
