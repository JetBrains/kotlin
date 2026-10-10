// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-90074
// RENDER_DIAGNOSTICS_FULL_TEXT

// FILE: test/Outer.java
package test;

public class Outer {}

// FILE: test/JavaApi.java
package test;

public class JavaApi {
    public Outer.Missing missing() {
        return null;
    }
}

// FILE: test/main.kt
package test

fun use(api: JavaApi) {
    api.<!MISSING_DEPENDENCY_CLASS!>missing<!>()
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType */
