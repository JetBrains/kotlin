// OPTIONS: true, false, false, false, true
// PARAM_DESCRIPTOR: local final fun bar(m: kotlin.Int): Int defined in foo
// PARAM_DESCRIPTOR: value-parameter val n: kotlin.Int defined in foo
// PARAM_TYPES: (kotlin.Int) -> Int
// PARAM_TYPES: kotlin.Int
fun foo(n: Int): Int {
    fun bar(m: Int) = m * n

    return <selection>bar(n + 1)</selection>
}