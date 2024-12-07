// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

import java.util.*

fun test(map: Map<Any, Any>) {
    HashMap(map)
}