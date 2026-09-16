// RUN_PIPELINE_TILL: CODEGEN
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@Suppress("DEPRECATION_ERROR")
fun foo() = immutableBlobOf()
