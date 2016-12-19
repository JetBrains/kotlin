fun printInt(x: Int) = println(x)

fun foo(arg: Int?) {
    printInt(arg ?: 16)
}

fun main(args: Array<String>) {
    foo(null)
    foo(42)
}