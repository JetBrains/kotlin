package f

fun h<R>(<!UNUSED_PARAMETER!>i<!>: Int, <!UNUSED_PARAMETER!>a<!>: Any, <!UNUSED_PARAMETER!>r<!>: R, <!UNUSED_PARAMETER!>f<!>: (Boolean) -> Int) = 1
fun h<R>(<!UNUSED_PARAMETER!>a<!>: Any, <!UNUSED_PARAMETER!>i<!>: Int, <!UNUSED_PARAMETER!>r<!>: R, <!UNUSED_PARAMETER!>f<!>: (Boolean) -> Int) = 1

fun test() = <!CANNOT_COMPLETE_RESOLVE!>h<!>(1, 1, 1, { <!CANNOT_INFER_PARAMETER_TYPE!>b<!> -> 42 })
