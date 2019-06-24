// FILE: J.java
public class J {
    public static Boolean BOOL_NULL = null;
    public static Boolean boolNull() { return null; }
}

// FILE: platformTypeReceiver.kt
fun test1() = J.BOOL_NULL.equals(null)
fun test2() = J.boolNull().equals(null)
