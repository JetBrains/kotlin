package streams.sequence.filter

fun main(args: Array<String>) {
    //Breakpoint!
    arrayOf(true, "12", false).asSequence().filterIsInstance<Boolean>().forEach {}
}