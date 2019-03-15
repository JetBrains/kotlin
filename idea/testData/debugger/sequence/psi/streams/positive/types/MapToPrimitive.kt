import java.util.stream.Stream

fun main(args: Array<String>) {
  <caret>  Stream.of(10, 200, 3000).mapToDouble { it * 1000.1 }.count()
}