// JVM_ABI_K1_K2_DIFF: KT-68087
// ISSUE: KT-50997

typealias MySuspendFunction = suspend (String) -> Unit
fun foo(function: MySuspendFunction) {}
fun box() = "OK"
