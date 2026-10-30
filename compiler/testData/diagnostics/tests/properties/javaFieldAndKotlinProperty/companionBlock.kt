// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks
// ISSUE: KT-90240

// FILE: Base.java
public class Base {
    int x;
}

// FILE: test.kt
class Location : Base() {
    fun readX(): Int = <!JAVA_FIELD_SHADOWED_BY_KOTLIN_PROPERTY!>x<!>
    companion {
        val x = -1
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, javaProperty, javaType,
propertyDeclaration */
