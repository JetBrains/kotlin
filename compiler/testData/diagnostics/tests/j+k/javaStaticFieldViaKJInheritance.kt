// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76426
// FIR_IDENTICAL
// FILE: MyInterface.java
public interface MyInterface {
    int MY_STATIC_FIELD = 1000;
}

// FILE: main.kt
interface MyInterfaceEx : MyInterface

fun main() {
    MyInterfaceEx.<!UNRESOLVED_REFERENCE!>MY_STATIC_FIELD<!>
}
