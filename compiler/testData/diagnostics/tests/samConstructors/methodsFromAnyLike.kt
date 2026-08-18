// RUN_PIPELINE_TILL: FRONTEND
// FILE: J.java
public interface J extends I {
    @Override
    String toString(String s);

    @Override
    int hashCode(String s);
}

// FILE: test.kt
interface I {
    fun foo()

    context(s: String)
    fun toString(): String

    context(s: String)
    fun hashCode(): Int
}

fun main() {
    val j = <!INTERFACE_AS_FUNCTION!>J<!> {}
    j.toString(<!NAMED_PARAMETER_NOT_FOUND!>s<!> = "")
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, interfaceDeclaration, javaFunction, javaType,
lambdaLiteral, localProperty, propertyDeclaration, stringLiteral */
