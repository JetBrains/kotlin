// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-90074
// RENDER_DIAGNOSTICS_FULL_TEXT
// USE_PSI_JAVA_FACADE
// Guards USE_PSI_JAVA_FACADE: a copy of kt90074QualifiedMissingNestedType.kt, whose expected diagnostic differs only because
// the PSI Java facade still splits the unresolved name at the last dot, giving the wrong ClassId 'Outer.Missing' (KT-90074)

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
