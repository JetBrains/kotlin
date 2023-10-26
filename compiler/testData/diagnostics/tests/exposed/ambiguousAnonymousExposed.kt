// ISSUE: KT-62918
// FIR_DUMP

class My<T>(val value: T)
interface I1
interface I2

fun <!EXPOSED_FUNCTION_RETURN_TYPE!>foo<!>() = My(object : I1, I2 {})

fun fooFoo(): My<I1> = My(object : I1, I2 {})

internal fun <!EXPOSED_FUNCTION_RETURN_TYPE!>bar<!>() = My(object : I1, I2 {})

private fun baz() = My(object : I1, I2 {})

// See also KT-33917
private <!NOTHING_TO_INLINE!>inline<!> fun <!PRIVATE_INLINE_FUNCTIONS_RETURNING_ANONYMOUS_OBJECTS!>bazBaz<!>() = My(object : I1, I2 {})

private <!NOTHING_TO_INLINE!>inline<!> fun <!PRIVATE_INLINE_FUNCTIONS_RETURNING_ANONYMOUS_OBJECTS!>bazBazBaz<!>() = My(object : I1 {})
