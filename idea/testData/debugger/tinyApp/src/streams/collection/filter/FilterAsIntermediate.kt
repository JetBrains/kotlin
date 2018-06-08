package streams.collection.filter

fun main(args: Array<String>) {
  // Breakpoint!
  listOf("abc", "bde", "gh").filter { it.length < 3 }.count()
}