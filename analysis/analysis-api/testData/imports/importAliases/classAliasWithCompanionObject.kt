// TYPE_ALIAS: as bar
import Foo as bar

class Foo {
    companion object {

    }
}

fun test() {
    val x: b<caret>ar? = null
}