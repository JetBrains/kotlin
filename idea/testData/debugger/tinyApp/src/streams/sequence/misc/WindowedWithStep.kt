package streams.sequence.misc

fun main(args: Array<String>) {
  //Breakpoint!
  sequenceOf(1, 1, 1, 1, 1, 1, 1, 1, 1).windowed(4, 2).count()
}