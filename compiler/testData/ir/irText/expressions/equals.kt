// FILE: equals.kt

fun testEqeq(a: Int, b: Int) = a == b
fun testEquals(a: Int, b: Int) = a.equals(b)

fun testJEqeqNull() = J.INT_NULL == null
fun testJEqualsNull() = J.INT_NULL.equals(null)

// FILE: J.java
public class J {
    public static Integer INT_NULL = null;
}
