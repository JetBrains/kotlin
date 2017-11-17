fun main(args: Array<String>) {
  listOf(20, 30).map { it.toStrin<caret>g().toByteOrNull() }.count()
}