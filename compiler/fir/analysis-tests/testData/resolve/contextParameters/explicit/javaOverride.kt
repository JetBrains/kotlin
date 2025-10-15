// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments
// FILE: J.java
public class J implements I {
    public void foo(String x) {}
}

// FILE: J2.java
public class J2 implements I, I2 {
    public void foo(String x) {}
}

// FILE: J3.java
public class J3 implements I, I3 {
    public void foo(String x) {}
}

// FILE: test.kt
interface I {
    context(s: String) fun foo()
}

interface I2 {
    context(s2: String) fun foo()
}

interface I3 {
    context(_: String) fun foo()
}

fun test(j: J, j2: J2, j3: J3) {
    j.foo(s = "")
    j2.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>s<!> = "")
    j2.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>s2<!> = "")
    j3.foo(s = "")
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, interfaceDeclaration, javaType,
stringLiteral */
