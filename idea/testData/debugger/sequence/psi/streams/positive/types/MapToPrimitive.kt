import java.util.stream.LongStream

fun main(args: Array<String>) {
  <caret>  LongStream.of(10, 200, 3000).mapToDouble { it * 1000.1 }.count()
}