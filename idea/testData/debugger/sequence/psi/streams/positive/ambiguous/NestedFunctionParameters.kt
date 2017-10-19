import java.util.stream.Stream

private fun function(a: Long, b: Long): Long {
  return a + b
}

fun main(args: Array<String>) {
<caret>  val c = function(Stream.of(1, 2).count(), function(Stream.of(1, 2, 3).count(), Stream.of(1, 2, 3, 4).count()))
}
