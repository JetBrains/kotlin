package misc

fun main(args: Array<String>) {
  // Breakpoint!
  intArrayOf(1, 1, 1, 1, 1, 1, 1).asSequence().windowed(3) { it.sum() }.count()
}