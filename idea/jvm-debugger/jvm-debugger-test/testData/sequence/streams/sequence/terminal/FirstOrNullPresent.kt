package streams.sequence.terminal

fun main(args: Array<String>) {
  //Breakpoint!
  sequenceOf(1, 3, 6, 3).firstOrNull { it == 3 }
}