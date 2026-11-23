package usage

fun main(args: Array<String>) {
    inline.Klass().f { println("to be inlined") }
}
