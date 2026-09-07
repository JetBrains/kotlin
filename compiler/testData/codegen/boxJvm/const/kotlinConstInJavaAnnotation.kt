// LANGUAGE: +IntrinsicConstEvaluation
// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: JAnno.java
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface JAnno {
    String name();
    int count();
    byte mask();
    char code();

    // Declared `long` so the unsigned value fits. See the call sites for the two
    // ways of getting the correct number into it.
    long bigDecoded();
}

// FILE: JHolder.java
@JAnno(
    name = KConst.TRIMMED,
    count = KConst.BUMPED,
    mask = KConst.MASKED,
    code = KConst.CODE,
    bigDecoded = KConst.BIG_U & 0xFFFFFFFFL
)
public class JHolder {}

// FILE: box.kt
import kotlin.experimental.and

object KConst {
    const val TRIMMED: String = "  OK  ".trim()
    const val BUMPED: Int = 41.inc()
    const val MASKED: Byte = (0x0F.toByte()) and (0x3C.toByte())
    const val CODE: Char = Char(41 + 24)

    // Does not fit in a signed Int. Kotlin's view is 4200000000; the JVM field is
    // an `int` holding -94967296, which is what Java reads without decoding.
    const val BIG_U: UInt = 4000000000u + 200000000u
}

fun box(): String {
    val a = JHolder::class.java.getAnnotation(JAnno::class.java) ?: return "Fail: no annotation"

    if (a.name != "OK") return "Fail name: ${a.name}"
    if (a.count != 42) return "Fail count: ${a.count}"
    if (a.mask != 0x0C.toByte()) return "Fail mask: ${a.mask}"
    if (a.code != 'A') return "Fail code: ${a.code}"
    // All three routes -- Java's mask, Kotlin's toLong() on a named const and
    // Kotlin's toLong() inline -- must agree on the unsigned value.
    if (a.bigDecoded != 4200000000L) return "Fail bigDecoded: ${a.bigDecoded}"
    if (KConst.BIG_U != 4200000000u) return "Fail BIG_U: ${KConst.BIG_U}"
    return "OK"
}
