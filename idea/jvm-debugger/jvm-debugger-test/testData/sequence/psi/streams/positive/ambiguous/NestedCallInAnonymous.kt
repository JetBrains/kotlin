import java.util.function.Consumer
import java.util.stream.Stream

fun main(args: Array<String>) {
  Stream.of(1)
      .peek(object : Consumer<Int> {
        override fun accept(x: Int) {
          <caret>          Stream.of(1).count()
        }
      }).forEach {  }
}
