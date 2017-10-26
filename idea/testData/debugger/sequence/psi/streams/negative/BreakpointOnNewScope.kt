import java.util.stream.IntStream

fun main(args: Array<String>) {
  <caret>run { val s = IntStream.range(1, 2).sum() }
}
