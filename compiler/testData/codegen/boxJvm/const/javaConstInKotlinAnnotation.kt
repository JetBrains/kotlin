// LANGUAGE: +IntrinsicConstEvaluation
// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: JSource.java
public class JSource {
    public static final String PADDED = "  OK  ";
    public static final int N = 41;
    public static final byte MASK = 0x0F;
}

// FILE: box.kt
import kotlin.experimental.and

@Retention(AnnotationRetention.RUNTIME)
annotation class KAnno(val name: String, val count: Int, val mask: Byte, val code: Char)

@KAnno(
    name = JSource.PADDED.trim(),
    count = JSource.N.inc(),
    mask = JSource.MASK and 0x3C.toByte(),
    code = Char(JSource.N + 24),
)
class KTarget

fun box(): String {

    val a = KTarget::class.java.getAnnotation(KAnno::class.java) ?: return "Fail: no annotation"
    if (a.name != "OK") return "Fail name: ${a.name}"
    if (a.count != 42) return "Fail count: ${a.count}"
    if (a.mask != 0x0C.toByte()) return "Fail mask: ${a.mask}"
    if (a.code != 'A') return "Fail code: ${a.code}"

    return "OK"
}
