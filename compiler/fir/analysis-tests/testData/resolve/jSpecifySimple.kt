// RUN_PIPELINE_TILL: FRONTEND
// FULL_JDK
// WITH_STDLIB
// ENABLE_FOREIGN_ANNOTATIONS

// FILE: Annotated.java

import org.jspecify.annotations.*;

public class Annotated {
    public static void takeNonNull(@NonNull String str) {
    }

    @Nullable
    public static String returnNullable() {
    }
}

// FILE: NullMarkedAnnotated.java

import org.jspecify.annotations.*;

@NullMarked
public class NullMarkedAnnotated {
    static void takeNonNull(String str) {
    }
}

// FILE: usages.kt

fun usages() {
    val x: String <!INITIALIZER_TYPE_MISMATCH!>=<!> Annotated.returnNullable()
    Annotated.takeNonNull(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    NullMarkedAnnotated.takeNonNull(<!NULL_FOR_NONNULL_TYPE!>null<!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, localProperty, nullableType, propertyDeclaration */
