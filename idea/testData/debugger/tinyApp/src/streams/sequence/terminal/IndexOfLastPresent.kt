package terminal

fun main(args: Array<String>) {
  //Breakpoint!
  sequenceOf(1, 2, 1, 2).indexOfLast { it == 1 }
}