// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-24210
// WITH_STDLIB

// KT-24210: CommonSupertypes does not account for type annotations when traversing the supertype tree

// FILE: C.java
import org.jetbrains.annotations.NotNull;

public interface C {
    @NotNull
    C getOriginal();
}

// FILE: kt24210.kt
fun <D : C> get(set: Set<D>, element: D) {
    set.contains(element.original)
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaProperty, javaType, typeConstraint, typeParameter */
