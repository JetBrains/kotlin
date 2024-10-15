// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

import java.util.*

fun takeJ(map: Map<Any, Any>) {}

fun test() {
    takeJ(HashMap())
}