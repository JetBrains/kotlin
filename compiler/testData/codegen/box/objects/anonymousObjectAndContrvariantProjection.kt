// ISSUE: KT-57222
// IGNORE_HEADER_MODE: JVM_IR
//   Reason: KT-82376

interface Invariant<A>

fun Invariant<in Number>.publicFunc() = privateFunc()

private fun <B> Invariant<B>.privateFunc() = object : Invariant<B> {}

fun box() = "OK"
