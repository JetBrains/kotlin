package my.simple.name
import my.simple.name.Foo.Companion.VARIABLE

class Foo {
    companion object {
        const val VARIABLE = 1
    }
}

fun main() {
    val a = my.simple.name.Foo<caret>.VARIABLE
    val b = my.simple.name.Foo.Companion.VARIABLE
    val c = my.simple.name.Foo()
}