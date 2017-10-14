import java.util.stream.Stream

fun main(args: Array<String>) {
  val a<caret> = 100
  Stream.of(1, 2, 3).toArray()
}
