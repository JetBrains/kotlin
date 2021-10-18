// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FULL_JDK

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 2 java/lang/invoke/LambdaMetafactory

// FILE: serializableLambdaToKotlinInterface.kt
import java.io.*

fun interface Sam : Serializable {
    fun get(s: String): String
}

fun box(): String {
    return roundtrip(Sam { s -> s + "K" })
        .get("O")
}

fun <T> roundtrip(x: T): T {
    val out1 = ByteArrayOutputStream()
    ObjectOutputStream(out1).writeObject(x)
    return ObjectInputStream(ByteArrayInputStream(out1.toByteArray())).readObject() as T
}
