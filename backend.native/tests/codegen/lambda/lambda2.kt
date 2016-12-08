fun main(args : Array<String>) {
    run {
        println(args[0])
    }
}

fun run(f: () -> Unit) {
    f()
}