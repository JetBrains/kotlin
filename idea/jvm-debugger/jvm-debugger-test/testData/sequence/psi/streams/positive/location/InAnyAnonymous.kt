import java.util.function.Consumer
import java.util.stream.Stream

fun main(args: Array<String>) {
  val count = Stream.of("abc", "acd", "ef").map({ it.length }).filter { x -> x % 2 == 0 }.peek(object : Consumer<Int> {
    override fun accept(x: Int) {
      prin<caret>tln(x)
    }
  }).count()
}
