// DISABLE_JAVA_FACADE
// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: MyEnum.java

public enum MyEnum {
    A;
    public int getOrdinal() {return "";}
}

// FILE: main.kt

fun foo() {
    MyEnum.A.getOrdinal()
}
