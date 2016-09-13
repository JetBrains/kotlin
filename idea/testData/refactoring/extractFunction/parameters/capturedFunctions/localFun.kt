// OPTIONS: true, false, false, false, true, false
// PARAM_DESCRIPTOR: local final fun bar(m: kotlin.Int): kotlin.Int defined in foo
// PARAM_DESCRIPTOR: value-parameter n: kotlin.Int defined in foo
// PARAM_TYPES: (m: kotlin.Int) -> kotlin.Int
// PARAM_TYPES: kotlin.Int
fun foo(n: Int): Int {
    fun bar(m: Int) = m * n

    return <selection>bar(n + 1)</selection>
}