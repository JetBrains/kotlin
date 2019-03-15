package streams.sequence.append

fun main(args: Array<String>) {
  //Breakpoint!
  listOf(1, 2, 3).asSequence().plus(arrayOf(3, 4, 5)).count()
}