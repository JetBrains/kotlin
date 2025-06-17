// APPROXIMATE_TYPE

interface Invariant<A>
private fun <B> Invariant<B>.privateFunc() = object : Invariant<B> {}
fun Invariant<in Number>.publicFunc() = <expr>privateFunc()</expr>