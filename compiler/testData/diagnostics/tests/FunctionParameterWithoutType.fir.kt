// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER -UNUSED_VARIABLE
fun test(a) {

}

class A(a<!SYNTAX!><!>)

val bar = fun(a){}

val la = { a -> }
val las = { a: Int -> }