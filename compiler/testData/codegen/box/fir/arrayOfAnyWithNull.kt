// TARGET_BACKEND: JVM_IR
// ISSUE: KT-77673

// FILE: TestJava.java
public class TestJava {
    public static Object getParent() {
        return null;
    }
}

// FILE: box.kt
fun box(): String {
    try {
        val result = arrayOf<Any>(TestJava.getParent())
        return result[0].toString()
    } catch (e: NullPointerException) {
        return "OK"
    }
}
