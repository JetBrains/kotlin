// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-76426
// FIR_IDENTICAL
// FILE: MyInterface.java
public interface MyInterface {
    int MY_STATIC_FIELD = 1000;
}

// FILE: MyInterfaceEx.java
public interface MyInterfaceEx extends MyInterface {
}

// FILE: main.kt
fun main() {
    MyInterfaceEx.MY_STATIC_FIELD
}
