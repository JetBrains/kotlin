package bar

@Suppress("REDECLARATION")
val foo: Int = 6

operator fun Int.invoke() = this

fun main(args: Array<String>) {
    println(foo())
}