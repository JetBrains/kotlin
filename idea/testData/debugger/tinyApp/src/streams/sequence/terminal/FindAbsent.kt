package streams.sequence.terminal

fun main(args: Array<String>) {
  //Breakpoint!
  sequenceOf(1, 2, 4).find { it % 5 == 0 }
}