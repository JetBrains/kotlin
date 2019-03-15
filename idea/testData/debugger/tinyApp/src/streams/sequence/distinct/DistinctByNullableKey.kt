package streams.sequence.distinct

fun main(args: Array<String>) {
  //Breakpoint!
  sequenceOf(1, 2, 3, 4, 5).distinctBy { if (it % 2 == 0) null else it }.count()
}
