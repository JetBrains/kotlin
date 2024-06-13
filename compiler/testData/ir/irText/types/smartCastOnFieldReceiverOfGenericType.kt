// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// FILE: smartCastOnFieldReceiverOfGenericType.kt
fun testSetField(a: Any, b: Any) {
    a as JCell<String>
    b as String
    a.value = b
}

fun testGetField(a: Any): String {
    a as JCell<String>
    return a.value
}

// FILE: JCell.java
public class JCell<T> {
    public T value;
}