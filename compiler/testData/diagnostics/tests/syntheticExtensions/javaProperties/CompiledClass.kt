// RUN_PIPELINE_TILL: CODEGEN
// FILE: KotlinFile.kt
import java.io.File

fun foo(file: File) {
    file.absolutePath.length
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaProperty */
