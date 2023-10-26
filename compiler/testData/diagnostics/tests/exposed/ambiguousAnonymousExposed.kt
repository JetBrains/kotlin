// ISSUE: KT-62918
// FIR_DUMP

class My<T>(val value: T)
interface I1
interface I2

fun <!EXPOSED_FUNCTION_RETURN_TYPE!>foo<!>() = My(object : I1, I2 {})

fun fooFoo(): My<I1> = My(object : I1, I2 {})

internal fun <!EXPOSED_FUNCTION_RETURN_TYPE!>bar<!>() = My(object : I1, I2 {})

private fun baz() = My(object : I1, I2 {})
