// DISABLE_JAVA_FACADE
// RUN_PIPELINE_TILL: CODEGEN
// FILE: A.java
@missing.Ann(x = "")
public class A {
    @missing.Ann(1)
    public String foo() {}
}

// FILE: main.kt

fun main() {
    A().foo().length
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType */
