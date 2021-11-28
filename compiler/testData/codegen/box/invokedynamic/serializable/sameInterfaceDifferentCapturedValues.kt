// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FULL_JDK

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 4 java/lang/invoke/LambdaMetafactory

// FILE: sameInterfaceDifferentCapturedValues.kt
import java.io.*

fun box(): String {
    val vo = "O"
    val vk = "K"

    val t1 = roundtrip(Sam { s -> s + vk }).get("O")
    if (t1 != "OK") return "Failed: t1='$t1'"

    val t2 = roundtrip(Sam { s -> vo + s + vk }).get("")
    if (t2 != "OK") return "Failed: t2='$t2'"

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
