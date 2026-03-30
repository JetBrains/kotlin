// RUN_PIPELINE_TILL: BACKEND
// FILE: A.java
public @interface A {
    Class<?> value() default Integer.class;
}

// FILE: b.kt
@A(String::class) class MyClass1
@A(value = String::class) class MyClass2
@A class MyClass3

/* GENERATED_FIR_TAGS: classDeclaration, classReference, javaType */
