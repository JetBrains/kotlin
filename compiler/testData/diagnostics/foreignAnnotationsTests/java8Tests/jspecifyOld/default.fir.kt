// FILE: NullnessUnspecifiedTypeParameter.java
import org.jspecify.nullness.*;

@NullMarked
public class NullnessUnspecifiedTypeParameter<T> {
    public void foo(T t) {}

    public void bar(Test s, T t) {} // t should not become not nullable
}

// FILE: Test.java
public class Test {}

// FILE: main.kt
// jspecify_nullness_mismatch
fun main(a1: NullnessUnspecifiedTypeParameter<Any>, a2: NullnessUnspecifiedTypeParameter<Any?>, x: Test): Unit {
    // jspecify_nullness_mismatch
    a1.foo(null)
    a1.foo(1)

    // jspecify_nullness_mismatch
    a2.foo(null)
    a2.foo(1)

    // jspecify_nullness_mismatch, jspecify_nullness_mismatch
    a1.bar(null, null)
    // jspecify_nullness_mismatch
    a1.bar(x, null)
    a1.bar(x, 1)

    // jspecify_nullness_mismatch, jspecify_nullness_mismatch
    a2.bar(null, null)
    // jspecify_nullness_mismatch
    a2.bar(x, null)
    a2.bar(x, 1)
}