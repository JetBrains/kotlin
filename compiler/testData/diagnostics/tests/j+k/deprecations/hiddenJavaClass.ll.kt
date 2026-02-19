// LL_FIR_DIVERGENCE
// KT-77114: Source Java classes cannot be hidden in the Analysis API
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: BACKEND
// FILE: JavaClass.java
public class JavaClass {
    @kotlin.Deprecated(message = "nested", level = kotlin.DeprecationLevel.HIDDEN)
    public static class Nested { }
}

// FILE: main.kt
class Nested {
    fun foo() = "OK"
}

class MyClass : JavaClass() {
    fun check(n: <!DEPRECATION_ERROR!>Nested<!>): String = n.<!UNRESOLVED_REFERENCE!>foo<!>()
}

fun box() = MyClass().check(<!ARGUMENT_TYPE_MISMATCH!>Nested()<!>)

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaType, stringLiteral */
