fun main(args: Array<String>) {
  lis<caret>tOf(Any(), null).asSequence().map { true }.contains(false)
}