import java.util.stream.Stream

fun main(args: Array<String>) {
  val count = Stream.of("abc", "acd", "ef").map(String:<caret>:length).filter { x -> x!! % 2 == 0 }.count()
}
