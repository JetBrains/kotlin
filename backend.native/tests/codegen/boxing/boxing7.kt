fun printInt(x: Int) = println(x)

fun foo(arg: Any) {
    val argAsInt = try {
        arg as Int
    } catch (e: ClassCastException) {
        0
    }
    printInt(argAsInt)
}

fun main(args: Array<String>) {
    foo(1)
    foo("Hello")
}