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

// FILE: J4.java
public class J4 implements I4 {
    public void foo(String x) {}
}

// FILE: J5.java
public class J5 implements I, I4 {
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

interface I4 : I2 {
    context(<!PARAMETER_NAME_CHANGED_ON_OVERRIDE!>s<!>: String) override fun foo()
}

fun test(j: J, j2: J2, j3: J3, j4: J4, j5: J5) {
    j.foo(s = "")
    j2.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>s<!> = "")
    j2.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>s2<!> = "")
    j3.foo(s = "")
    j4.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>s<!> = "") // should be ok
    j4.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>s2<!> = "") // should be ok
    j5.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>s<!> = "") // should be ok
    j5.foo(<!NAME_FOR_AMBIGUOUS_PARAMETER!>s2<!> = "") // should be ok
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, interfaceDeclaration, javaType,
stringLiteral */
