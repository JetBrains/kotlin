fun foo(vararg args: Any?) {
    for (arg in args) {
        println(arg.toString())
    }
}

fun main(args: Array<String>) {
    foo(1, null, true, "Hello")
}