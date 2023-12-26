// ISSUE: KT-50997
// JVM_ABI_K1_K2_DIFF: KT-63877

typealias MySuspendFunction = suspend (String) -> Unit
fun foo(function: MySuspendFunction) {}
fun box() = "OK"
