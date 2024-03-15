// FIR_IDENTICAL
package f

fun <R> h(i: Int, a: Any, r: R, f: (Boolean) -> Int) = 1
fun <R> h(a: Any, i: Int, r: R, f: (Boolean) -> Int) = 1

fun test() = <!OVERLOAD_RESOLUTION_AMBIGUITY!>h<!>(1, 1, 1, { <!CANNOT_INFER_PARAMETER_TYPE!>b<!> -> 42 })
