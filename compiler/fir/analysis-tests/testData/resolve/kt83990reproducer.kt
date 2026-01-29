// RUN_PIPELINE_TILL: FRONTEND

// WITH_STDLIB
// FULL_JDK
// ENABLE_FOREIGN_ANNOTATIONS

// FILE: JavaClass.java
import org.jspecify.annotations.NullMarked;

@NullMarked
public class JavaClass {
    static void of(String s) {
    }
}

// FILE: main.kt

fun main(args: Array<String>) {
    JavaClass.of(<!NULL_FOR_NONNULL_TYPE!>null<!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, nullableType */
