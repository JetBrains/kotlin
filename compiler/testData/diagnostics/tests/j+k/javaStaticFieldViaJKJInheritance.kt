// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-76426
// FIR_IDENTICAL
// FILE: MyInterface.java
public interface MyInterface {
    int MY_STATIC_FIELD = 1000;
}

// FILE: MyInterfaceImpl.java
public class MyInterfaceImpl implements MyInterfaceEx {
}

// FILE: main.kt
interface MyInterfaceEx : MyInterface

fun main() {
    MyInterfaceImpl.MY_STATIC_FIELD
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, javaProperty, javaType */
