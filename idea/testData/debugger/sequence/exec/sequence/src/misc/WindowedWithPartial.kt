package misc

fun main(args: Array<String>) {
  // Breakpoint!
  listOf(1, 1, 1, 1, 1).windowed(3, partialWindows = true) { it.size }.count()
}