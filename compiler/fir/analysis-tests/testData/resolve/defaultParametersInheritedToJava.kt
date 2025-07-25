// RUN_PIPELINE_TILL: BACKEND
// FILE: main.kt

fun foo(j: MyJavaClass) {
    j.bar()
}

abstract class MyClass {
    open fun bar(y: String? = null) {}
}

// FILE: MyJavaClass.java

public class MyJavaClass extends MyClass {
    public void bar(String y) {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaFunction, javaType, nullableType */
