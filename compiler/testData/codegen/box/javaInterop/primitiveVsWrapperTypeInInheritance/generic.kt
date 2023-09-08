// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// IGNORE_LIGHT_ANALYSIS
// FILE: box.kt
class E : D()

fun box(): String =
    E().foo(0.0)

// FILE: A.java
public interface A<T> {
    default String foo(T value) {
        return "Fail: A";
    }
}

// FILE: B.java
public abstract class B implements A<Double> {
    public String foo(double value) {
        return "OK";
    }
}

// FILE: C.kt
open class C : B()

// FILE: D.java
public class D extends C {
    @Override
    public String foo(Double value) {
        return "Fail: D";
    }
}
