// ISSUE: KT-50997

typealias MySuspendFunction = suspend (String) -> Unit
fun foo(function: MySuspendFunction) {}
fun box() = "OK"