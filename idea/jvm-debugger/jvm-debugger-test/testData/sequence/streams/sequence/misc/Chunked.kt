package streams.sequence.misc

fun main(args: Array<String>) {
  //Breakpoint!
  (0..5).asSequence().chunked(4).count()
}