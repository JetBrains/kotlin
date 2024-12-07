// TARGET_BACKEND: JVM
// FILE: A.java
public class A {
    public static String o = "O";

    public static String k() {
        return "K";
    }
}

// FILE: B.kt
open class B : A()

// FILE: C.kt
open class C : B()

// FILE: D.java
public class D extends C {}

// FILE: E.kt
class E : D() {
    fun g(): String = o + k()
}

// FILE: box.kt
fun box(): String = E().g()
