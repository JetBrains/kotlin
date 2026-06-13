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
    foo(JavaClass<*>::baz)
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, functionalType, javaCallableReference */
