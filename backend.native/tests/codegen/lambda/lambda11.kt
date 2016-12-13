fun main(args : Array<String>) {
    val first = "first"
    val second = "second"

    run {
        println(first)
        println(second)
    }
}

fun run(f: () -> Unit) {
    f()
}