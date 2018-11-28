package streams.sequence.filter

fun main(args: Array<String>) {
    //Breakpoint!
    byteArrayOf(1, 2, 3, 2).asSequence().minus(arrayOf(2.toByte())).count()
}