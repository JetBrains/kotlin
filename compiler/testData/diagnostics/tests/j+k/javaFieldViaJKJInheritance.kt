// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-76426
// FIR_IDENTICAL
// FILE: MyClass.java
public class MyClass {
    int myField = 1000;
}

// FILE: MyClassImpl.java
public class MyClassImpl extends MyClassEx {
}

// FILE: main.kt
open class MyClassEx : MyClass()

fun main(j: MyClassImpl) {
    j.myField
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaProperty, javaType */
