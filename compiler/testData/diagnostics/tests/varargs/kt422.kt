// KT-422 Tune literal typing rules so that varargs overloaded by primitive types work

fun <T> foo(vararg t : T) = t
fun foo(vararg a: Int) = a
fun foo(vararg a: Long) = a

fun test() {
    foo(1, 2, 3) // Error, but should be foo of ints
}

fun <T> array(vararg elements : T) = elements
fun array(vararg elements : Int) = elements

fun test1() {
    array("A", "A")
    array(1, 1)
}