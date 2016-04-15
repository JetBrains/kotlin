// "Create type parameter 'X'" "true"
open class Foo(x: <caret>X)

class Bar : Foo(1)

fun test() {
    Foo(1)
    Foo("2")

    object : Foo("2") {

    }
}