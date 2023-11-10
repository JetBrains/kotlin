// FIR_IDENTICAL
// WITH_PLATFORM_LIBS
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import kotlinx.cinterop.*
import platform.posix.*

fun foo() = stat(malloc(42u)!!.rawValue)
