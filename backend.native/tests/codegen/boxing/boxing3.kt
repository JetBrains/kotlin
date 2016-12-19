fun printInt(x: Int) = println(x)

fun foo(arg: Int?) {
    if (arg != null)
        printInt(arg)
}

fun main(args: Array<String>) {
    foo(42)
}