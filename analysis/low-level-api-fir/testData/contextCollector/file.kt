package <expr>test</expr>

import java.util.List

fun test() = List::class

fun other(): Int {
    return "foo".length
}

class Foo {
    fun foo() {
        require(other() == 3)
    }
}