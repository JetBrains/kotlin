package streams.sequence.map

fun main(args: Array<String>) {
  //Breakpoint!
  intArrayOf(1, 2, 3, 4).asSequence().mapIndexed { ix, _ -> ix }.count()
}