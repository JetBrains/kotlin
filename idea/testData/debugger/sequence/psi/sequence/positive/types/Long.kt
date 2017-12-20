fun main(args: Array<String>) {
  longArrayOf(1L, 2L<caret>).asSequence().count { it < 2 }
}