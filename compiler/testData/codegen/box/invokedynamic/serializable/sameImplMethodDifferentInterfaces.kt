// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FULL_JDK

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 4 java/lang/invoke/LambdaMetafactory

// FILE: sameImplMethodDifferentInterfaces.kt
import java.io.*

fun plusK(s: String) = s + "K"

fun box(): String {
    val sam1 = roundtrip(Sam1(::plusK))
    if (sam1 !is Sam1) return "Failed: $sam1 !is Sam1"
    val t1 = sam1.get("O")
    if (t1 != "OK") return "Failed: $t1='$t1'"

    val sam2 = roundtrip(Sam2(::plusK))
    if (sam2 !is Sam2) return "Failed: $sam2 !is Sam2"
    val t2 = sam2.get("O")
    if (t2 != "OK") return "Failed: $t2='$t2'"

    return "OK"
}

fun <T> roundtrip(x: T): T {
    val out1 = ByteArrayOutputStream()
    ObjectOutputStream(out1).writeObject(x)
    return ObjectInputStream(ByteArrayInputStream(out1.toByteArray())).readObject() as T
}

// FILE: Sam1.java
import java.io.*;

public interface Sam1 extends Serializable {
    String get(String s);
}

// FILE: Sam2.java
import java.io.*;

public interface Sam2 extends Serializable {
    String get(String s);
}
