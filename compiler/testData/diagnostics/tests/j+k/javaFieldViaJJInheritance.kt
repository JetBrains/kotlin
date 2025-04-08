// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-76426
// FIR_IDENTICAL
// FILE: MyClass.java
public class MyClass {
    int myField = 1000;
}

// FILE: MyClassEx.java
public class MyClassEx extends MyClass {
}

// FILE: main.kt
fun main(j: MyClassEx) {
    j.myField
}
