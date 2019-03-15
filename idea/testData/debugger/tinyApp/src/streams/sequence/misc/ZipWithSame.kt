package streams.sequence.misc

fun main(args: Array<String>) {
  //Breakpoint!
  sequenceOf(1, 2).zip(sequenceOf(3, 2)).sumBy { it.second }
}