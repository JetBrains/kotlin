package streams.sequence.misc

fun main(args: Array<String>) {
  //Breakpoint!
  sequenceOf(1, 2, 3).zip(sequenceOf(1)).contains(5 to 1)
}