open class Foo {}
open class Bar {}

fun <T : Bar, T1> foo(x : Int) {}
fun <T1, T : Foo> foo(x : Long) {}

fun f(): Unit {
    <error descr="[NONE_APPLICABLE] None of the following functions are applicable: [/foo, /foo]">foo</error><Int, Int>(1)
}
