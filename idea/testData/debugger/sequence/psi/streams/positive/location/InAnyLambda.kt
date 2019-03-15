import java.util.stream.Stream

object Bar {
  @JvmStatic
  fun main(args: Array<String>) {
    val count = Stream.of("abc", "acd", "ef").map({ it.length }).filter { x -> <caret>x % 2 == 0 }.count()
  }
}
