// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// IGNORE_LIGHT_ANALYSIS
// FILE: box.kt
class E : D()

fun box(): String =
    E().foo(0)

// FILE: A.java
import org.jetbrains.annotations.Nullable;

public interface A {
    default String foo(@Nullable Integer value) {
        return "Fail: A";
    }
}

// FILE: B.java
public abstract class B implements A {
    public String foo(int value) {
        return "OK";
    }
}

// FILE: C.kt
open class C : B()

// FILE: D.java
import org.jetbrains.annotations.Nullable;

public class D extends C {
    @Override
    public String foo(@Nullable Integer value) {
        return "Fail: D";
    }
}
