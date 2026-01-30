// RUN_PIPELINE_TILL: FRONTEND
// FULL_JDK
// WITH_STDLIB
// ENABLE_FOREIGN_ANNOTATIONS

// FILE: JavaClass.java

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class JavaClass {
    static JavaClass ofJspecify(@NonNull String... args) {
        return new JavaClass();
    }

    static JavaClass ofJetbrains(@NotNull String... args) {
        return new JavaClass();
    }
}

// FILE: main.kt

fun main() {
    JavaClass.ofJetbrains(null) // this is green, because String! expected
    JavaClass.ofJspecify(<!NULL_FOR_NONNULL_TYPE!>null<!>) // this is red
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction */
