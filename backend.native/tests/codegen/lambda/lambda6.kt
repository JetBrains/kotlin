fun main(args : Array<String>) {
    val str = "captured"
    foo {
        println(it)
        println(str)
    }
}

fun foo(f: (Int) -> Unit) {
    f(42)
}