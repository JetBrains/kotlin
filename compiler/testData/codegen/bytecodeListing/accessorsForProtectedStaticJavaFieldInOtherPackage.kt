// FILE: C.kt
package test

class C: A.B() {
    // For binary compatibility, two accessibility bridges should be generated in C:
    // one for A.x and one for A.B.x.
    // Otherwise, if a static 'x' field is added to A.B either A.x or A.B.x will be ignored.
    // The JVM backend generates accessibility bridges for setters as well which is not necessary.
    fun f() = ({ A.x + x })()
    // Similarly for static functions. Two bridges should be generated for binary compatibility.
    fun g() = ({ A.h() + h() })
}

// FILE: A.java
public class A {
    protected static String x = "O";
    protected static String h() { return "O"; }
    public static class B extends A {

    }
}

