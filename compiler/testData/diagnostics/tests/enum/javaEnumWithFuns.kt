// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: A.java
public enum A {
    ENTRY,
    ANOTHER;
    
    public String s() {
        return "";
    }
}

// FILE: test.kt

fun main() {
    val c = A.ENTRY
    c.s()
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaProperty, javaType, localProperty,
propertyDeclaration */
