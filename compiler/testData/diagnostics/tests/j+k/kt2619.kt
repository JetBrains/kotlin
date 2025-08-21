// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +WarnAboutNonExhaustiveWhenOnAlgebraicTypes
// FILE: foo.kt
fun main() {
    val c: Type
    <!NO_ELSE_IN_WHEN!>when<!> (<!UNINITIALIZED_VARIABLE!>c<!>)  {

    }
}



//FILE: Type.java
public enum Type {
    TYPE,
    NO_TYPE;
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaType, localProperty, propertyDeclaration, whenExpression,
whenWithSubject */
