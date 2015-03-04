// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
fun test(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>a<!>) {

}

class A(a<!SYNTAX!><!>)

val bar = fun test(<!CANNOT_INFER_PARAMETER_TYPE!>a<!>){}

val la = { (<!CANNOT_INFER_PARAMETER_TYPE!>a<!>) -> }
val las = { (a: Int) -> }