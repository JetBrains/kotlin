// LL_FIR_DIVERGENCE
// `JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE` is not reported for declarations of a dependency module analyzed from sources:
// `FirJvmModuleAccessibilityQualifiedAccessChecker` skips source symbols (KT-90960), and the LL test platform's
// `CliJavaModuleResolver` has no user modules, so source files don't belong to their `module-info.java` module.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
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

// FILE: foo/impl/Impl.java
package foo.impl;

public class Impl {}

// MODULE: moduleB1(moduleA)
// ADDITIONAL_JAVA_MODULES: moduleA
// FILE: usage.kt
import foo.Foo
import foo.impl.Impl

fun usage() {
    Foo()

    Impl()
}

// FILE: Usage.java
public class Usage {
    public static void main(String[] args) {
        new foo.Foo();
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction */
