import java.util.stream.Collectors
import java.util.stream.Stream

fun main(args: Array<String>) {
  val cou<caret>nt = Stream
      .of(1, 2, 3).distinct().collect(Collectors.toList())
      .stream().sorted().collect(Collectors.toList())
      .stream().count()
}
