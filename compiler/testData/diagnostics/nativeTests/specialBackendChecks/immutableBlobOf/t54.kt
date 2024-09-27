// FIR_IDENTICAL
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@Suppress("DEPRECATION_ERROR")
fun foo(arr: ShortArray) = immutableBlobOf(*arr)
