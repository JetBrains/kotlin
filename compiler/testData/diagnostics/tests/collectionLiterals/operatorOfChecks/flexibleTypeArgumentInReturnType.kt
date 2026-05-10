// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals

// FILE: Intermediate.java

import org.jetbrains.annotations.NotNull;

public class Intermediate {
    @NotNull
    static KotlinClass<String> flexibleKotlinClass() {
        return new KotlinClass<String>();
    }

    @NotNull
    static ReversedClass<String> flexibleReversedClass() {
        return new ReversedClass<String>();
    }
}

// FILE: Main.kt

class KotlinClass<D> {
    companion object {
        operator fun of() = Intermediate.flexibleKotlinClass()
        operator fun of(vararg ps: String): KotlinClass<String> = KotlinClass()
    }
}

class ReversedClass<D> {
    companion object {
        operator fun of(vararg ps: String) = Intermediate.flexibleReversedClass()
        operator fun of(): ReversedClass<String> = ReversedClass()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, flexibleType, functionDeclaration, javaFunction, nullableType,
objectDeclaration, operator, typeParameter, vararg */
