package streams.sequence.terminal

fun main(args: Array<String>) {
  //Breakpoint!
  sequenceOf(2, 3, 4, 7, 5).partition { it % 2 == 0 }
}