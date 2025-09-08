// RUN_PIPELINE_TILL: BACKEND
// FILE: Derived.kt

class Derived : Some()

// FILE: Some.java

public class Some implements Strange {
    public Object foo() {
        return "";
    }
}

// FILE: Strange.kt

interface Strange<out T> {
    fun foo(): T
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, javaType, nullableType, out,
typeParameter */
