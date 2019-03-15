import java.util.stream.Stream

fun main(args: Array<String>) {
  val count = Stream.of("abc", "acd", "ef").map(String::length).count()
  val af<caret>ter = 20
}
