// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FULL_JDK


// CHECK_BYTECODE_TEXT
// 16 java/lang/invoke/LambdaMetafactory
// 1 (LOOKUP|TABLE)SWITCH
// 8 java/lang/String\.equals

// FILE: multipleTopLevelFunRefs.kt

// No deduplication happens now, because each reference would generate it's own private
// wrapper function. In principle, it's possible to deduplicate such functions,
// at least in simple cases, where no adaptations happen. Than there would be only 4 private
// funcions instead of 8, and only 4 cases in desirialization switch, leading to only 12 indy calls.

import java.io.*

fun plusK1(s: String) = s + "K"
fun plusK2(s: String) = s + "K"
fun plusK3(s: String) = s + "K"
fun plusK4(s: String) = s + "K"

fun box(): String {
    val t1 = roundtrip(Sam(::plusK1)).get("O")
    if (t1 != "OK") return "Failed: t1='$t1'"

    val t1a = roundtrip(Sam(::plusK1)).get("O")
    if (t1a != "OK") return "Failed: t1a='$t1a'"

    val t1b = roundtrip(Sam(::plusK1)).get("O")
    if (t1b != "OK") return "Failed: t1b='$t1b'"

    val t2 = roundtrip(Sam(::plusK2)).get("O")
    if (t2 != "OK") return "Failed: t2='$t2'"

    val t2a = roundtrip(Sam(::plusK2)).get("O")
    if (t2a != "OK") return "Failed: t2a='$t2a'"

    val t3 = roundtrip(Sam(::plusK3)).get("O")
    if (t3 != "OK") return "Failed: t3='$t3'"

    val t4 = roundtrip(Sam(::plusK4)).get("O")
    if (t4 != "OK") return "Failed: t4='$t4'"

    val t4a = roundtrip(Sam(::plusK4)).get("O")
    if (t4a != "OK") return "Failed: t4a='$t4a'"

    return "OK"
}

fun <T> roundtrip(x: T): T {
    val out1 = ByteArrayOutputStream()
    ObjectOutputStream(out1).writeObject(x)
    return ObjectInputStream(ByteArrayInputStream(out1.toByteArray())).readObject() as T
}

// FILE: Sam.java
import java.io.*;

public interface Sam extends Serializable {
    String get(String s);
}
