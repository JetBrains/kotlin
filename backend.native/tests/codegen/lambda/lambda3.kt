fun main(args : Array<String>) {
    var str = "lambda"
    run {
        println(str)
    }
}

fun run(f: () -> Unit) {
    f()
}