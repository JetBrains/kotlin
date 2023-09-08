// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// IGNORE_LIGHT_ANALYSIS
// FILE: box.kt
class E : D()

fun box(): String =
    E().foo(0)

// FILE: A.java
public interface A {
    default String foo(Integer value) {
        return "Fail: A";
    }
}

// FILE: K.kt
interface K : A

// FILE: B.java
public abstract class B implements K {
    public String foo(int value) {
        return "OK";
    }
}

// FILE: C.kt
open class C : B()

// FILE: D.java
public class D extends C {
    @Override
    public String foo(Integer value) {
        return "Fail: D";
    }
}
