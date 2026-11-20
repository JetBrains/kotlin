// TARGET_BACKEND: JVM
// FILE: receiverOfIntersectionType.kt

interface K

interface I : K {
    fun ff()
}

interface J : K {}

class A: I, J {
    override fun ff() {}
}

class B: I, J {
    override fun ff() {}
}

fun testIntersection(a: A, b: B) {
    val v = if (true) a else b
    v.ff()
}

fun testFlexible1() {
    val v = if (true) Java.a() else Java.b()
    v.ff()
}

fun testFlexible2(a: A, b: B) {
    val v = if (true) Java.id(a) else Java.id(b)
    v.ff()
}

// FILE: Java.java
public class Java {
    public static A a() { return new A(); }
    public static B b() { return new B(); }
    public static <T> T id(T x) { return x; }
}
