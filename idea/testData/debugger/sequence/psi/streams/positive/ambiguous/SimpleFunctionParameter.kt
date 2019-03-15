import java.util.stream.Stream

private fun func(a: Long, b: Long): Long {
  return a + b
}

fun main(args: Array<String>) {
<caret>  val c = func(Stream.of(1, 2).count(), Stream.of(1, 2, 3).count())
}
