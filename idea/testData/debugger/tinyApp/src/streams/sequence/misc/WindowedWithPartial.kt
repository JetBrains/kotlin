package streams.sequence.misc

fun main(args: Array<String>) {
  //Breakpoint!
  listOf(1, 1, 1, 1, 1).asSequence().windowed(3, partialWindows = true, transform = { it.size }).count()
}