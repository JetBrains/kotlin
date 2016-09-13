// OPTIONS: true, false, false, false, true, false
// PARAM_DESCRIPTOR: local final fun kotlin.Int.bar(m: kotlin.Int): kotlin.Int defined in foo
// PARAM_DESCRIPTOR: value-parameter n: kotlin.Int defined in foo
// PARAM_TYPES: kotlin.Int.(m: kotlin.Int) -> kotlin.Int
// PARAM_TYPES: kotlin.Int
fun foo(n: Int): Int {
    fun Int.bar(m: Int) = this * m * n

    return <selection>n bar (n + 1)</selection>
}