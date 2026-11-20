// LL_FIR_DIVERGENCE
// LL FIR diagnostics tests don't run the JVM backend, so its diagnostics are not reported: `CONFLICTING_INHERITED_JVM_DECLARATIONS`.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
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

interface I : A, B

/* GENERATED_FIR_TAGS: interfaceDeclaration, javaType */
