// FIR_IDENTICAL
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import kotlinx.cinterop.*

fun bar(x: Int) = x.convert<String>()
