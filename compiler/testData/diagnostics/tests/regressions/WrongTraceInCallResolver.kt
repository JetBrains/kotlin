open class Foo {}
open class Bar {}

fun <T : Bar, T1> foo(<!UNUSED_PARAMETER!>x<!> : Int) {}
fun <T1, T : Foo> foo(<!UNUSED_PARAMETER!>x<!> : Long) {}

fun f(): Unit {
    foo<<!UPPER_BOUND_VIOLATED!>Int<!>, Int>(1)
}
