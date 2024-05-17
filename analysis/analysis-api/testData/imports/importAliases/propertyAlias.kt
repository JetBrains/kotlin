// TYPE_ALIAS: bar
import Foo.foo as bar

object Foo {
    val foo = 1
}

fun test() {
    val x = b<caret>ar
}