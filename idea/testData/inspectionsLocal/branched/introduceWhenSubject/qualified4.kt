class Foo {
    val bar = 1
}

const val A = 1
const val B = 2

fun test(foo: Foo): Int {
    return <caret>when {
        A == foo.bar -> 10
        B == foo.bar -> 20
        else -> 30
    }
}