// OPTIONS: true, false, false, false, true
// PARAM_DESCRIPTOR: local final fun baz(m: kotlin.Int): Int defined in foo.bar
// PARAM_DESCRIPTOR: value-parameter val n: kotlin.Int defined in foo
// PARAM_TYPES: (kotlin.Int) -> Int
// PARAM_TYPES: kotlin.Int

// SIBLING:
fun foo(n: Int): Int {
    fun bar(): Int {
        fun baz(m: Int) = m * n

        return <selection>baz(n + 1)</selection>
    }

    return bar()
}