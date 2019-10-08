package streams.sequence.distinct

fun main(args: Array<String>) {
  //Breakpoint!
  val seq = (22..25).map { it * it }.asSequence()
      .distinctBy { it.toString().first() }
      .count()
}