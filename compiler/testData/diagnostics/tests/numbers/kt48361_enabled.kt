// LANGUAGE: +ApproximateIntegerLiteralTypesInReceiverPosition

fun foo(ttlMillis: Long = <!TYPE_MISMATCH!>5 * 60 * 1000<!>) {}

const val cacheSize: Long = <!TYPE_MISMATCH!>4096 * 4<!>

