// LL_FIR_DIVERGENCE
// The LL test platform doesn't model the module path: `moduleA` is a regular dependency of the unnamed module even
// though it is not added with `-Xadd-modules`, so `foo.Foo` is resolved.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// Test that although we have moduleA in the module path, it's not in the module graph
// because we did not provide -Xadd-modules=moduleA.

// JDK_KIND: FULL_JDK_11
// FORCE_COMPILE_AS_JAVA_MODULE
// MODULE: moduleA
// FILE: module-info.java
module moduleA {
    exports foo;
}

// FILE: foo/Foo.java
package foo;

public class Foo {}

// MODULE: moduleB(moduleA)
// FILE: usage.kt
import foo.Foo

fun usage() {
    Foo()
}

/* GENERATED_FIR_TAGS: functionDeclaration */
