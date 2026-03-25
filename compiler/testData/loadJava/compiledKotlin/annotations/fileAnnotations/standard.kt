// IGNORE_FIR_METADATA_LOADING_K1: ANY
// IGNORE_FIR_METADATA_LOADING_K2: JVM_IR
// ^ Serializing file annotations to metadata is not implemented in Kotlin/JVM. See OSIP-1095.

@file:Suppress("UNUSED_PARAMETER")
@file:OptIn(ExperimentalStdlibApi::class)

package test

fun foo(x: Int) = 42