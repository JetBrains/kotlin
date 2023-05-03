open class Foo {}
open class Bar {}

fun <T : Bar, T1> foo(x : Int) {}
fun <T1, T : Foo> foo(x : Long) {}

fun f(): Unit {
    foo<<!UPPER_BOUND_VIOLATED!>Int<!>, Int>(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
}
