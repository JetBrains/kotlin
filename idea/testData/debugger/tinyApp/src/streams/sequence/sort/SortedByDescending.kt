package streams.sequence.sort

fun main(args: Array<String>) {
  //Breakpoint!
  arrayOf(Person("Bob", 42), Person("Alice", 27)).asSequence().sortedByDescending { it.age }.count()
}