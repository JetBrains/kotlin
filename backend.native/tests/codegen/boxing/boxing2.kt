fun printInt(x: Int) = println(x)
fun printBoolean(x: Boolean) = println(x)

fun foo(arg: Any) {
    if (arg is Int)
        printInt(arg)
    else if (arg is Boolean)
        printBoolean(arg)
    else
        println("other")
}

fun main(args: Array<String>) {
    foo(1)
    foo(true)
    foo("Hello")
}