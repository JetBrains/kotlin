// LL_FIR_DIVERGENCE
// LL FIR diagnostics tests don't run the JVM backend, so its diagnostics are not reported: `ACCIDENTAL_OVERRIDE`.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +ContextParameters
// IGNORE_ERRORS
// FILE: JavaBase.java
public interface JavaBase {
    public String foo(String a, String b);
}

// FILE: KotlinInterface.kt
import JavaBase

interface KotlinInterface : JavaBase {
    context(a: String)
    fun foo(b: String): String
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, interfaceDeclaration, javaType */
