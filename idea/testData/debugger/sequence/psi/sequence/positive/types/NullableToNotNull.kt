fun main(args: Array<String>) {
  listOf(0.4, null).asSequence().<caret>map { 10 }.count()
}