// RUN_PIPELINE_TILL: FRONTEND
// WITH_PLATFORM_LIBS

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import platform.posix.*

fun main() {
    println()
    println(<!CALLABLE_REFERENCES_TO_VARIADIC_C_FUNCTIONS_ARE_NOT_SUPPORTED!>::printf<!>)
}
