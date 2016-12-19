fun printInt(x: Int) = println(x)

fun foo(arg: Any?) {
    if (arg is Int? && arg != null)
        printInt(arg)
}

fun main(args: Array<String>) {
    foo(16)
}