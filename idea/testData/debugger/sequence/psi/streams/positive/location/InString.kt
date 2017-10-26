import java.util.stream.Stream

fun main(args: Array<String>) {
  val count = Stream.of("abc", "ac<caret>d", "ef").map({ it.length }).filter { x -> x % 2 == 0 }.count()
}
