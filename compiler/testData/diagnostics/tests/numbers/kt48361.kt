// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

fun foo(ttlMillis: Long = 5 * 60 * 1000) {}

const val cacheSize: Long = 4096 * 4
