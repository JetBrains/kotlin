// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass<String>) {
    javaClass.something += "x"
}

// FILE: JavaClass.java
public class JavaClass<T> {
    public T getSomething() { return null; }
    public void setSomething(T value) { }
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, flexibleType, functionDeclaration, javaProperty, javaType,
localProperty, propertyDeclaration, stringLiteral */
