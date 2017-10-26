import java.util.stream.Stream

fun main(args: Array<String>) {
  object : Runnable {
    override fun run() {
<caret>      Stream.of(1, 2, 3).forEach { x -> }
    }
  }.run()
}
