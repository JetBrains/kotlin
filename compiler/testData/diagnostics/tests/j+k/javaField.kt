// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-76426
// FILE: MyClass.java
public class MyClass {
    int myField = 1000;
}

// FILE: main.kt
fun main(j: MyClass) {
    j.myField
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaProperty, javaType */
