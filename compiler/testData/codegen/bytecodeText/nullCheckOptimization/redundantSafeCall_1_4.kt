// !LANGUAGE: -SafeCallsAreAlwaysNullable
// IGNORE_BACKEND_K2: JVM_IR
// Status: Feature is always on in K2. See KT-62930

fun test(s: String) = s?.length

// 0 IFNULL
// 0 IFNONNULL
// 0 intValue
// 0 valueOf
