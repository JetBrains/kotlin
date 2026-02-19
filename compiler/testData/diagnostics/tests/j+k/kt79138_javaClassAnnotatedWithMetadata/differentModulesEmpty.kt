// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// MODULE: lib
// FILE: E.java
import kotlin.Metadata;

@Metadata()
public enum E {
    OK;
}

// MODULE: main(lib)
// FILE: box.kt
fun box(): String = E.OK.name

/* GENERATED_FIR_TAGS: functionDeclaration */
