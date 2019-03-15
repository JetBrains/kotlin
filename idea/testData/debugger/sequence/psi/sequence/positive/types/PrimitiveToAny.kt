fun main(args: Array<String>) {
  listOf(20, 30).asSequence().m<caret>ap { Any() }.contains(Any())
}