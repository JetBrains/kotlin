// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-76426
// FIR_IDENTICAL
// FILE: MyClass.java
public class MyClass {
    int myField = 1000;
}

// FILE: main.kt
fun main(j: MyClass) {
    j.myField
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaProperty, javaType */
