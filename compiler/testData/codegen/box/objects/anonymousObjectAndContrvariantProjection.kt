// ISSUE: KT-57222

interface Invariant<A>

fun Invariant<in Number>.publicFunc() = privateFunc()

private fun <B> Invariant<B>.privateFunc() = object : Invariant<B> {}

fun box() = "OK"
