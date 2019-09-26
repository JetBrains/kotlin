package streams.sequence.filter

fun main(args: Array<String>) {
    //Breakpoint!
    intArrayOf(1, 2, 3).asSequence().filterIndexed({ index, _ -> index % 2 == 0 }).count()
}