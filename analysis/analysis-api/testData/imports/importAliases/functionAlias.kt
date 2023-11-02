// TYPE_ALIAS: as bar
import Foo.foo as bar

object Foo {
    fun foo() {}
}

fun test() {
    b<caret>ar()
}