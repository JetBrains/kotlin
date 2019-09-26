import java.util.function.Consumer
import java.util.stream.Stream

fun main(args: Array<String>) {
  val `var` = "abd"
  val count = Stream.of(`va<caret>r`, "acd", "ef").map({ it.length }).filter { x -> x % 2 == 0 }.peek(object : Consumer<Int> {
    override fun accept(x: Int) {
      println(x)
    }
  }).count()
}
