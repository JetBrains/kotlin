// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals

// FILE: Intermediate.java

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Intermediate {
    static <T> T flexible() {
        return null;
    }

    @Nullable
    static <T> T nullable() {
        return null;
    }

    @NotNull
    static <T> T notNull() {
        return null;
    }
}

// FILE: Main.kt

class KotlinClass {
    companion object {
        operator fun <!POTENTIALLY_NULLABLE_RETURN_TYPE_OF_OPERATOR_OF!>of<!>() = Intermediate.flexible<KotlinClass>() // KotlinClass!
        operator fun <!INCONSISTENT_RETURN_TYPES_IN_OF_OVERLOADS, NULLABLE_RETURN_TYPE_OF_OPERATOR_OF!>of<!>(p1: String) = Intermediate.nullable<KotlinClass>() // KotlinClass?
        operator fun of(vararg ps: String) = Intermediate.notNull<KotlinClass>() // KotlinClass
    }
}

class ReversedClass {
    companion object {
        operator fun of() = Intermediate.notNull<ReversedClass>()
        operator fun <!POTENTIALLY_NULLABLE_RETURN_TYPE_OF_OPERATOR_OF!>of<!>(vararg ss: String) = Intermediate.flexible<ReversedClass>()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, flexibleType, functionDeclaration, javaFunction, nullableType,
objectDeclaration, operator, vararg */
