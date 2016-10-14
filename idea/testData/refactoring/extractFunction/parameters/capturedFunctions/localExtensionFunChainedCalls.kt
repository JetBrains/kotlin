// OPTIONS: true, false, false, false, true, false
// PARAM_DESCRIPTOR: local final fun kotlin.Int.bar1(m: kotlin.Int): kotlin.Int defined in foo
// PARAM_DESCRIPTOR: local final fun kotlin.Int.bar2(m: kotlin.Int): kotlin.Int defined in foo
// PARAM_DESCRIPTOR: value-parameter n: kotlin.Int defined in foo
// PARAM_TYPES: kotlin.Int.(m: kotlin.Int) -> kotlin.Int
// PARAM_TYPES: kotlin.Int.(m: kotlin.Int) -> kotlin.Int
// PARAM_TYPES: kotlin.Int
fun foo(n: Int): Int {
    fun Int.bar1(m: Int) = this + m + n
    fun Int.bar2(m: Int) = this * m * n

    return <selection>n.bar1(n + 1).bar2(n + 2)</selection>
}