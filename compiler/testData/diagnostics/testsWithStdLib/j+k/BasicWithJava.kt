// RUN_PIPELINE_TILL: CODEGEN
// FILE: Some.java

public class Some {

}

// FILE: jvm.kt

class A : Some()

/* GENERATED_FIR_TAGS: classDeclaration, javaType */
