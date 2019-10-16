import java.util.stream.Stream

fun main(args: Array<String>) {
  ({ <caret>Stream.of(1, 2, 3).forEach { x -> } } as Runnable).run()
}
