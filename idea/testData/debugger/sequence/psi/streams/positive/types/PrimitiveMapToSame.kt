import java.util.stream.LongStream

fun main(args: Array<String>) {
  <caret>  LongStream.of(1, 2, 3).flatMap { LongStream.range(0, 10) }.count()
}