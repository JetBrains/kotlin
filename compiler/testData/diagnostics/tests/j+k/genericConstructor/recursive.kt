// RUN_PIPELINE_TILL: FRONTEND
// FILE: C.java

// See KT-10410
public class C {
    public <T extends T> C(T t) {
    }
}

// FILE: main.kt

fun foo() = <!CANNOT_INFER_PARAMETER_TYPE, NO_VALUE_FOR_PARAMETER!>C<!>()

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaType */
