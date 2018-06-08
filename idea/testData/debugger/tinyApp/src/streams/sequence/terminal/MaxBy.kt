package streams.sequence.terminal

fun main(args: Array<String>) {
  //Breakpoint!
  sequenceOf("abc", "bc").maxBy { it.length }
}