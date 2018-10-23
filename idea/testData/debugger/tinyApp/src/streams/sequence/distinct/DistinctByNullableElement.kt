package streams.sequence.distinct

fun main(args: Array<String>) {
  //Breakpoint!
  sequenceOf(null, 1, 2, 3, null).distinctBy { it == null }.count()
}