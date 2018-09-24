package streams.collection.distinct

fun main(args: Array<String>) {
  // Breakpoint!
  listOf(1, 2, 3, 4, 5).distinctBy { it % 2 }
}