// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// FILE: implicitNotNullOnPlatformType.kt
fun f(s: String) {}

class MySet : Set<String> {
    override val size: Int get() = TODO()
    override fun contains(element: String): Boolean = TODO()
    override fun containsAll(elements: Collection<String>): Boolean = TODO()
    override fun isEmpty(): Boolean = TODO()
    override fun iterator(): Iterator<String> = TODO()
}

fun test() {
    f(J.s())
    f(J.STRING)
}

fun testContains(m: MySet) {
    m.contains(J.STRING)
    m.contains("abc")
}

// FILE: J.java
public class J {
    public static String STRING = s();
    public static String s() { return null; }
}
