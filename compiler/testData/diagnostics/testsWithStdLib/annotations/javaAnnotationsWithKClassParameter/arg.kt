// RUN_PIPELINE_TILL: CODEGEN
// FILE: A.java
public @interface A {
    Class<?> arg();
}

// FILE: b.kt
@A(arg = String::class) class MyClass3

/* GENERATED_FIR_TAGS: classDeclaration, classReference, javaType */
