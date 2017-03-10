inline fun foo(block: () -> Unit) {
    block()
}

fun bar(block: () -> Unit) {
    foo(block)
}

fun main(args: Array<String>) {
    bar {
        println("OK")
    }
}