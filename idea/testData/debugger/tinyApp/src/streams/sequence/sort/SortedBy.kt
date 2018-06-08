package streams.sequence.sort

fun main(args: Array<String>) {
  //Breakpoint!
  arrayOf(Person("Bob", 42), Person("Alice", 27)).asSequence().sortedBy { it.age }.count()
}

data class Person(val name: String, val age: Int)