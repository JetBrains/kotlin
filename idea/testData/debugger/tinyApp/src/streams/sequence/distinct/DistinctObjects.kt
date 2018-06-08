package streams.sequence.distinct

fun main(args: Array<String>) {
  fun str(chr: Char): String = String(charArrayOf(chr))
  //Breakpoint!
  listOf(str('a'), str('b'), str('a'), str('c'), str('b')).asSequence().distinct().count()
}