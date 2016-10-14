// OPTIONS: true, false, false, false, true, false
// PARAM_DESCRIPTOR: local final fun baz(m: kotlin.Int): kotlin.Int defined in foo.bar
// PARAM_DESCRIPTOR: value-parameter n: kotlin.Int defined in foo
// PARAM_TYPES: (m: kotlin.Int) -> kotlin.Int
// PARAM_TYPES: kotlin.Int

// SIBLING:
fun foo(n: Int): Int {
    fun bar(): Int {
        fun baz(m: Int) = m * n

        return <selection>baz(n + 1)</selection>
    }

    return bar()
}