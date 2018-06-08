package streams.sequence.filter

fun main(args: Array<String>) {
    //Breakpoint!
    doubleArrayOf(1.0, 3.0, 5.0).asSequence().takeWhile { it < 2 }.forEach {}
}