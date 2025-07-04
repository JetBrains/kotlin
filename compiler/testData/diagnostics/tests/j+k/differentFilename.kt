// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-4455
// SKIP_JAVAC

// FILE: A.java
public class A {
    public B b() {}
    public F f() {}
}

class B { public void bar() {} }

// FILE: C.java
class D {
    public void baz() {}
}

// FILE: E.java
class F {
    public void foobaz() {}
}

// FILE: main.kt
fun main(x: A) {
    x.b().bar()
    x.<!MISSING_DEPENDENCY_CLASS!>f<!>().<!UNRESOLVED_REFERENCE!>foobaz<!>()

    <!UNRESOLVED_REFERENCE!>D<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>baz<!>()
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType */
