// !WITH_NEW_INFERENCE
package f

fun <R> h(i: Int, a: Any, r: R, f: (Boolean) -> Int) = 1
fun <R> h(a: Any, i: Int, r: R, f: (Boolean) -> Int) = 1

fun test() = <!CANNOT_COMPLETE_RESOLVE{OI}, OVERLOAD_RESOLUTION_AMBIGUITY{NI}!>h<!>(1, 1, 1, { <!CANNOT_INFER_PARAMETER_TYPE{OI}!>b<!> -> 42 })
