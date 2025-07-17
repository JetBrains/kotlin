// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FILE: E.java
import kotlin.Metadata;

@Metadata()
public enum E {
    OK;
}

// FILE: box.kt
fun box(): String = E.OK.name

/* GENERATED_FIR_TAGS: functionDeclaration */
