// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FULL_JDK

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 2 java/lang/invoke/LambdaMetafactory

// FILE: serializableSam2.kt
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
public interface Ok extends Base, Marker {}

// FILE: Base.java
public interface Base {
    String ok();
}

// FILE: Marker.java
import java.io.Serializable;

public interface Marker extends Serializable {}
