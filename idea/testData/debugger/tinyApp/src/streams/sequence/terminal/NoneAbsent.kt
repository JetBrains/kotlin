package streams.sequence.terminal

fun main(args: Array<String>) {
  //Breakpoint!
  sequenceOf(1, 2, 3).none { it == 3 }
}