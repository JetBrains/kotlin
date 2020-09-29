// IGNORE_BACKEND_FIR: JVM_IR
fun test(s: String) = s?.length

// 0 IFNULL
// 0 IFNONNULL
// 0 intValue
// 0 valueOf
