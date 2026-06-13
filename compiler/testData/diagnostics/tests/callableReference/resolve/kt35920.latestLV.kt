// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER
// LATEST_LV_DIFFERENCE
// FILE: JavaClass.java

public class JavaClass<R> {
    public static String baz(int x) { return ""; }
}

// FILE: main.kt
fun foo(x: (Int) -> String) {}

fun main() {
    foo(<!INVALID_QUALIFIER_IN_LHS_OF_CALLABLE_REFERENCE_TO_STATIC_WARNING!>JavaClass<*><!>::baz)
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, functionalType, javaCallableReference */
