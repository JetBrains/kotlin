package streams.sequence.flatMap

fun main(args: Array<String>) {
  //Breakpoint!
  listOf(listOf(1,2,3), listOf(), listOf(4,5,6)).asSequence().flatten().toList()
}