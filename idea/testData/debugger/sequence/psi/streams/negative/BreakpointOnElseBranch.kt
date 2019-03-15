import java.util.stream.IntStream

fun main(args: Array<String>) {
  if (args.isEmpty()) {
    val s = IntStream.range(1, 2).sum()
<caret>  } else {
    val s = IntStream.range(1, 5).sum()
  }
}