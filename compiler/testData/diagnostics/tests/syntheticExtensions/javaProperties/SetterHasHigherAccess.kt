// RUN_PIPELINE_TILL: FRONTEND
// FILE: KotlinFile.kt
package k

import JavaClass

fun foo(javaClass: JavaClass) {
    val v = javaClass.<!INVISIBLE_MEMBER!>something<!>
    javaClass.<!INVISIBLE_MEMBER!>something<!> = 1
    javaClass.<!INVISIBLE_MEMBER!>something<!>++
}

// FILE: JavaClass.java
public class JavaClass {
    protected int getSomething() { return 1; }
    public void setSomething(int value) {}
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, incrementDecrementExpression, integerLiteral, javaProperty,
javaType, localProperty, propertyDeclaration */
