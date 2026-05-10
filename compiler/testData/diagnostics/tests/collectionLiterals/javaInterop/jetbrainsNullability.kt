// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals

// FILE: VarargIsFlexible1.java
import org.jetbrains.annotations.NotNull;

public interface VarargIsFlexible1 {
    public static VarargIsFlexible1 of(String str, @NotNull String... strs) {
        return null;
    }
}

// FILE: VarargIsFlexible2.java
import org.jetbrains.annotations.NotNull;

public interface VarargIsFlexible2 {
    public static VarargIsFlexible2 of(@NotNull String str, @NotNull String... strs) {
        return null;
    }
}

// FILE: VarargIsFlexible3.java
import org.jetbrains.annotations.Nullable;

public interface VarargIsFlexible3 {
    public static VarargIsFlexible3 of(@Nullable String str) {
        return null;
    }

    public static VarargIsFlexible3 of(@Nullable String... strs) {
        return null;
    }
}

// FILE: main.kt

fun main() {
    val v1: VarargIsFlexible1 = <!UNRESOLVED_REFERENCE!>[""]<!>
    val v2: VarargIsFlexible2 = <!UNRESOLVED_REFERENCE!>[""]<!>
    val v3: VarargIsFlexible3 = <!UNRESOLVED_REFERENCE!>[null]<!>
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, javaType, localProperty, propertyDeclaration,
stringLiteral */
