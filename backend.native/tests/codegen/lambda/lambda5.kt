fun main(args : Array<String>) {
    foo {
        println(it)
    }
}

fun foo(f: (Int) -> Unit) {
    f(42)
}