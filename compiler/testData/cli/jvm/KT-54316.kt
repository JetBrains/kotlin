class Foo {
    companion object {
        fun bar() {
            println("Invoked")
        }
    }
}

fun main() {
    val a = Foo::bar
    a.invoke()
    a.invoke(Foo())
}