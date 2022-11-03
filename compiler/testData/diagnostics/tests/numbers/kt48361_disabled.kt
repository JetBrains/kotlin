// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
// !LANGUAGE: -ApproximateIntegerLiteralTypesInReceiverPosition

fun foo(ttlMillis: Long = 5 * 60 * 1000) {}

const val cacheSize: Long = 4096 * 4
