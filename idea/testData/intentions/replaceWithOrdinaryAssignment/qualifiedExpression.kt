class Foo {
    var x: Int = 1
}

fun main() {
    val foo = Foo()
    foo.x <caret>+= 1
}