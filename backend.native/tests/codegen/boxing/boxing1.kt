fun foo(arg: Any) {
    println(arg.toString())
}

fun main(args: Array<String>) {
    foo(1)
    foo(false)
    foo("Hello")
}