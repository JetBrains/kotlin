// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FULL_JDK

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 2 java/lang/invoke/LambdaMetafactory

// FILE: serializableSam1.kt
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

fun roundtrip(r: Ok): String {
    val output = ByteArrayOutputStream()
    ObjectOutputStream(output).writeObject(r)
    val input = ByteArrayInputStream(output.toByteArray())
    val rr = ObjectInputStream(input).readObject() as Ok
    return rr.ok()
}

fun box() = roundtrip { "OK" }

// FILE: Ok.java
import java.io.Serializable;

public interface Ok extends Serializable {
    String ok();
}
