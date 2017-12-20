fun main(args: Array<String>) {
  <caret> listOf("abc", 12).asSequence().map { 10 }.count()
}