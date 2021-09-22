// SKIP_TXT
// !LANGUAGE: -EnhanceNullabilityOfPrimitiveArrays
// FILE: J.java
import org.jetbrains.annotations.Nullable;

public interface J {
    int @Nullable [] foo();
}

// FILE: main.kt
fun bar(j: J) = <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>j.foo()<!>.iterator()
