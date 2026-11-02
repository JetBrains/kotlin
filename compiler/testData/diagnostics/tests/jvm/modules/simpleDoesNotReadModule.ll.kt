// LL_FIR_DIVERGENCE
// The LL test platform's `CliJavaModuleResolver` has no user modules, so source files do not belong to their
// `module-info.java` modules and `JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE` is not reported.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-90960
// JDK_KIND: FULL_JDK_11

// MODULE: moduleA
// FILE: module-info.java
module moduleA {
    exports a;
}

// FILE: a/A.java
package a;

public class A {}

// MODULE: moduleB(moduleA)
// FILE: module-info.java
module moduleB {
    requires kotlin.stdlib;
}

// FILE: usage.kt
import a.A

fun usage() {
    A()
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction */
