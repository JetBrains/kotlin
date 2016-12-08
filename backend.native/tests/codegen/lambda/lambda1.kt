fun main(args : Array<String>) {
    run {
        println("lambda")
    }
}

fun run(f: () -> Unit) {
    f()
}