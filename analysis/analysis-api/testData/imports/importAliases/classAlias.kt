// TYPE_ALIAS: as bar
import Foo as bar

object Foo {
}

fun test() {
    val x: b<caret>ar? = null
}