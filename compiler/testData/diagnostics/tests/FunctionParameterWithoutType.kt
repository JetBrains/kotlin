// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun test(a<!SYNTAX!><!>) {

}

class A(a<!SYNTAX!><!>)

val bar = fun test(a<!SYNTAX!><!>){}

val la = { (<!CANNOT_INFER_PARAMETER_TYPE!>a<!>) -> }
val las = { (a: Int) -> }