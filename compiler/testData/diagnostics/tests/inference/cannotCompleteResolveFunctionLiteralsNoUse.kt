// !WITH_NEW_INFERENCE
package f

fun <R> h(<!UNUSED_PARAMETER!>i<!>: Int, <!UNUSED_PARAMETER!>a<!>: Any, <!UNUSED_PARAMETER!>r<!>: R, <!UNUSED_PARAMETER!>f<!>: (Boolean) -> Int) = 1
fun <R> h(<!UNUSED_PARAMETER!>a<!>: Any, <!UNUSED_PARAMETER!>i<!>: Int, <!UNUSED_PARAMETER!>r<!>: R, <!UNUSED_PARAMETER!>f<!>: (Boolean) -> Int) = 1

fun test() = <!CANNOT_COMPLETE_RESOLVE!>h<!>(1, 1, 1, { <!CANNOT_INFER_PARAMETER_TYPE, UNUSED_ANONYMOUS_PARAMETER!>b<!> -> 42 })
