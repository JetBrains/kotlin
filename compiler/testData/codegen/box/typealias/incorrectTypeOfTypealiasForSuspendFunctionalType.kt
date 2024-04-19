// JVM_ABI_K1_K2_DIFF: KT-65038
// ISSUE: KT-50997

typealias MySuspendFunction = suspend (String) -> Unit
fun foo(function: MySuspendFunction) {}
fun box() = "OK"
