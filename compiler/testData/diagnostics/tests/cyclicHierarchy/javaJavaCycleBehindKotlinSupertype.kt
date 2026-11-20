// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-67708
// FILE: main.kt
fun test(j: J) {
    j.<!UNRESOLVED_REFERENCE!>k<!>()
}

// FILE: J.java
public class J implements K {
}

// FILE: K.kt
interface K : J2 {
    fun k() {}
}

// FILE: J2.java
public interface J2 extends J3 {
}

// FILE: J3.java
public interface J3 extends J2 {
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, javaType */
