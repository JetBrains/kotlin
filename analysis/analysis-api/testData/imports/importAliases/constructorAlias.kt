// TYPE_ALIAS: bar
import Foo as bar

class Foo() {
    constructor(i: Int): this()
}

fun test() {
    val foo = ba<caret>r()
}