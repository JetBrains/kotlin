// RUN_PIPELINE_TILL: BACKEND
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@Suppress("DEPRECATION_ERROR")
fun foo(x: Short) = immutableBlobOf(x)
