package streams.sequence.misc

fun main(args: Array<String>) {
  //Breakpoint!
  listOf(1, 2).asSequence().zip(sequenceOf(2, 1, 5)) { old, new -> old + new }.sum()
}