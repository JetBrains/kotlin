import java.util.stream.LongStream

fun main(args: Array<String>) {
  <caret>  LongStream.of(1, 2, 3).mapToInt { it.toInt() }.count()
}