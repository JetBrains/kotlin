package usage

fun main(args: Array<String>) {
    test.f {
        println("to be inlined")
    }
}
