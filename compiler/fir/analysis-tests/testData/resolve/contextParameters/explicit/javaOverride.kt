// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitPassingOfContextParameters
// FILE: J.java
public class J implements I {
    public void foo(String x) {}
}

// FILE: J2.java
public class J2 implements I, I2 {
    public void foo(String x) {}
}

// FILE: test.kt
interface I {
    context(s: String) fun foo()
}

interface I2 {
    context(s2: String) fun foo()
}

fun test(j: J, j2: J2) {
    j.foo(s = "")
    j2.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>s<!> = "")
    j2.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>s2<!> = "")
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, interfaceDeclaration, javaType,
stringLiteral */
