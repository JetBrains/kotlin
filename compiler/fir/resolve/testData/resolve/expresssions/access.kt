
class Foo {
    val x = 1

    fun abc() = x

    fun cba() = abc()
}

class Bar {
    val x = ""

    fun Foo.abc() = x
}

fun Foo.ext() = x

fun bar() {

}

fun buz() {
    bar()
}