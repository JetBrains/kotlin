import java.util.stream.Stream

fun main(args: Array<String>) {
  <caret>val count = Stream.of("abc", "acd", "ef").map({ it.length }).filter { x -> x % 2 == 0 }.count()
}
