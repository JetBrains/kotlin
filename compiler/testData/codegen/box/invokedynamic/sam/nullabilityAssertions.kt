// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

// FILE: nullabilityAssertions.kt
fun box(): String {
    try {
        A.bar {}
    } catch (e: NullPointerException) {
        return "OK"
    }

    return "Should throw NullPointerException"
}

// FILE: A.java
import org.jetbrains.annotations.NotNull;

public interface A {
    void foo(@NotNull String s);

    static void bar(A a) {
        a.foo(null);
    }
}
