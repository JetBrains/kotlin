// !WITH_NEW_INFERENCE
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class test

fun foo(@test f : Int) {}

var bar : Int = 1
    set(@test v) {}

val x : (Int) -> Int = <!INITIALIZER_TYPE_MISMATCH{LT}!>{@test x <!SYNTAX!>: Int -> x<!>}<!> // todo fix parser annotation on lambda parameter

class Hello(@test args: Any) {
}
