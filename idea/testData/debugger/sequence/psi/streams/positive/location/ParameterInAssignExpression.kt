import java.util.stream.IntStream

internal fun bar2(a: Int, b: Int, c: Int): Int {
  return 0
}

internal fun foo2(): Int {
<caret>  val result = bar2(0, 1, c = IntStream.range(1, 2).sum())
  return result
}

fun main(args: Array<String>) {
  foo2()
}
