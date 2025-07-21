// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// WITH_STDLIB
// MODULE: lib
// FILE: E.java
import kotlin.Metadata;

@Metadata(k = 1)
public enum E {
    OK;
}

// MODULE: main(lib)
// FILE: box.kt
fun box(): String = <!UNRESOLVED_REFERENCE!>E<!>.OK.name

/* GENERATED_FIR_TAGS: functionDeclaration */
