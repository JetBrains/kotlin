fun foo(vararg args: Any?) {
    for (arg in args) {
        println(arg.toString())
    }
}

fun bar(vararg args: Any?) {
    foo(1, *args, 2, *args, 3)
}

fun main(args: Array<String>) {
    bar(null, true, "Hello")
}