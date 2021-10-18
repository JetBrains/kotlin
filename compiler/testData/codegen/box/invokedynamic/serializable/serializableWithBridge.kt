// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY
// FULL_JDK

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 2 java/lang/invoke/LambdaMetafactory

// FILE: serializableWithBridge.kt
import java.io.*

fun plusK(s: String) = s + "K"

fun box(): String {
    return roundtrip(Sam(::plusK))
        .get("O")
}

fun <T> roundtrip(x: T): T {
    val out1 = ByteArrayOutputStream()
    ObjectOutputStream(out1).writeObject(x)
    return ObjectInputStream(ByteArrayInputStream(out1.toByteArray())).readObject() as T
}

// FILE: Base1.java
public interface Base1<T1> {
    String get(T1 s);
}

// FILE: Base2.java
public interface Base2<T2 extends CharSequence> {
    String get(T2 s);
}

// FILE: Sam.java
public interface Sam extends Base1<String>, Base2<String>, java.io.Serializable {
}
