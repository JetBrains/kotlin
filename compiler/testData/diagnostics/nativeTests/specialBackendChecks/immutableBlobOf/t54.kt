// FIR_IDENTICAL
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@Suppress("DEPRECATION")
fun foo(arr: ShortArray) = immutableBlobOf(*arr)
