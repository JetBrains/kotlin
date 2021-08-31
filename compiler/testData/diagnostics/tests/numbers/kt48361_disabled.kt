// !LANGUAGE: -ApproximateIntegerLiteralTypesInReceiverPosition

fun foo(ttlMillis: Long = <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>5 * 60 * 1000<!>) {}

const val cacheSize: Long = <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>4096 * 4<!>
