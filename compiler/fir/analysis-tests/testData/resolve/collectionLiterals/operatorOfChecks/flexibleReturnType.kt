// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals

// FILE: Intermediate.java

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Intermediate {
    static KotlinClass flexible() {
        return null;
    }

    @NotNull
    static KotlinClass notNull() {
        return new KotlinClass();
    }

    @Nullable
    static KotlinClass nullable() {
        return null;
    }
}
// FILE: Main.kt

class KotlinClass {
    companion object {
        operator fun of() = Intermediate.flexible()
        operator fun of(p1: String) = Intermediate.notNull()
        operator fun <!NULLABLE_RETURN_TYPE_OF_OPERATOR_OF!>of<!>(vararg ps: String) = Intermediate.nullable()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, flexibleType, functionDeclaration, javaFunction, nullableType,
objectDeclaration, operator, vararg */
