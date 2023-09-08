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

// FILE: B.java
public interface B extends A {
    default String foo(int value) {
        return "OK";
    }
}

// FILE: AImpl.java
public abstract class AImpl implements A {}

// FILE: BImpl.java
public abstract class BImpl extends AImpl implements B {}

// FILE: C.kt
open class C : BImpl()

// FILE: D.java
public class D extends C {
    @Override
    public String foo(Integer value) {
        return "Fail: D";
    }
}
