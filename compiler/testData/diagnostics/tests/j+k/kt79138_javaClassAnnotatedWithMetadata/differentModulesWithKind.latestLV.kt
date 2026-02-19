// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
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
fun box(): String = E.OK.name

/* GENERATED_FIR_TAGS: functionDeclaration */
