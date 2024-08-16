// ISSUE: KT-62918
// FIR_DUMP

class My<T>(val value: T)
interface I1
interface I2

fun <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>foo<!>() = My(object : I1, I2 {})

fun fooFoo(): My<I1> = My(object : I1, I2 {})

internal fun <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>bar<!>() = My(object : I1, I2 {})

private fun baz() = My(object : I1, I2 {})

// See also KT-33917
private <!NOTHING_TO_INLINE!>inline<!> fun <!AMBIGUOUS_ANONYMOUS_TYPE_INFERRED!>bazBaz<!>() = My(object : I1, I2 {})

private <!NOTHING_TO_INLINE!>inline<!> fun bazBazBaz() = My(object : I1 {})
