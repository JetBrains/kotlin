fun printInt(x: Int) = println(x)

class Foo(val value: Int?) {
    fun foo() {
        printInt(if (value != null) value else 42)
    }
}

fun main(args: Array<String>) {
    Foo(17).foo()
    Foo(null).foo()
}