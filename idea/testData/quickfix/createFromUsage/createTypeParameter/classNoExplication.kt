// "Create type parameter 'X' in class 'Foo'" "true"
open class Foo(x: <caret>X)

class Bar : Foo(1)

fun test() {
    Foo(1)
    Foo("2")

    object : Foo("2") {

    }
}