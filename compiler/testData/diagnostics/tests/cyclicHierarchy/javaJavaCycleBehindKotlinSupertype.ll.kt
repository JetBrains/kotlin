// LL_FIR_DIVERGENCE
// KT-67708: the loop in supertypes of J is missed while K is not resolved yet
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-67708
// FILE: main.kt
fun test(j: J) {
    j.k()
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
