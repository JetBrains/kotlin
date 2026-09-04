// RUN_PIPELINE_TILL: BACKEND
// FILE: A.java
import org.jetbrains.annotations.*;

public interface A {
    void foo(@Nullable String x);
}

// FILE: B.java
import org.jetbrains.annotations.*;

public interface B {
    void foo(@NotNull String x);
}

// FILE: C.kt

interface <!CONFLICTING_INHERITED_JVM_DECLARATIONS!>I<!> : A, B

/* GENERATED_FIR_TAGS: interfaceDeclaration, javaType */
