// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
// IGNORE_ERRORS
// FILE: JavaInterface.java
public interface JavaInterface {
    String foo(String a, String b);
    String getB(String a);
}

// FILE: KotlinContextInterface.kt
interface KotlinContextInterface {
    context(a: String)
    fun foo(b: String): String

    context(a: String)
    val b: String
}

// FILE: test.kt
interface <!CONFLICTING_INHERITED_JVM_DECLARATIONS!>Intersection<!> : KotlinContextInterface, JavaInterface

interface IntersectionWithOverride : KotlinContextInterface, JavaInterface {
    <!ACCIDENTAL_OVERRIDE!>context(a: String)
    override fun foo(b: String): String<!>

    context(a: String)
    override val b: String
        <!ACCIDENTAL_OVERRIDE!>get()<!> = ""
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, getter, interfaceDeclaration, javaType,
override, propertyDeclaration, propertyDeclarationWithContext, stringLiteral */
