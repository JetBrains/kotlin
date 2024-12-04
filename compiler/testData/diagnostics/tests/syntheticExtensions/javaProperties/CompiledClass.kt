// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: KotlinFile.kt
import java.io.File

fun foo(file: File) {
    file.absolutePath.length
}
