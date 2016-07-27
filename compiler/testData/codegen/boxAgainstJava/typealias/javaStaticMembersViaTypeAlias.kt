// FILE: JTest.java
public class JTest {
    public static String o() { return "O"; }
    public static final String K = "K";
}

// FILE: 1.kt
typealias JT = JTest

fun box(): String = JT.o() + JT.K