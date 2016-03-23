// INTENTION_TEXT: "Add import for 'ppp.Foo.Bar'"
// WITH_RUNTIME
package ppp

sealed class Foo {
    class Bar(val x: Int) : Foo()
}

fun test() {
    val foo = Foo.<caret>Bar(5)

    when (foo) {
        is Foo.Bar -> println(foo.x)
    }
}
