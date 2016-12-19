fun printInt(x: Int) = println(x)

fun foo(arg: Any) {
    printInt(arg as? Int ?: 16)
}

fun main(args: Array<String>) {
    foo(42)
    foo("Hello")
}