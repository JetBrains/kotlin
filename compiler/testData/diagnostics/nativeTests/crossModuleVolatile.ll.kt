// LL_FIR_DIVERGENCE
// We report `LEAKED_VOLATILE_FIELD` after fir2ir stage before serialization in Klib
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: BACKEND
// DISABLE_IR_VISIBILITY_CHECKS: ANY
// DIAGNOSTICS: -ERROR_SUPPRESSION

// MODULE: lib
// FILE: lib.kt
@file:OptIn(kotlin.ExperimentalStdlibApi::class)

import kotlin.native.concurrent.*
import kotlin.concurrent.*

class Box(@Volatile var value: String)

// MODULE: main(lib)
// FILE: main.kt

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import kotlin.native.concurrent.*
import kotlin.concurrent.*

fun box() : String {
    val o = "O"
    val x = Box(o)
    return x::value.compareAndExchangeField(o, "K") + x.value
}
