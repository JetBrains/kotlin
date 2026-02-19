// TYPE_ALIAS: bar
import Foo as bar

object Foo {
}

fun test() {
    val x: b<caret>ar? = null
}