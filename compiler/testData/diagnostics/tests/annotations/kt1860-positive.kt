// !WITH_NEW_INFERENCE
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class test

fun foo(@test <!UNUSED_PARAMETER!>f<!> : Int) {}

var bar : Int = 1
    set(@test <!UNUSED_PARAMETER!>v<!>) {}

val x : (Int) -> Int = {@test <!TYPE_MISMATCH, UNINITIALIZED_VARIABLE!>x<!> <!SYNTAX!>: Int -> x<!>} // todo fix parser annotation on lambda parameter

class Hello(@test <!UNUSED_PARAMETER!>args<!>: Any) {
}
