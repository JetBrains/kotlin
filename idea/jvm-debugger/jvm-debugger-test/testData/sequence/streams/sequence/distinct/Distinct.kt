package streams.sequence.distinct

fun main(args: Array<String>) {
  //Breakpoint!
  listOf(1, 2, 3, 2, 1, 3, 4, 2).asSequence().distinct().count()
}