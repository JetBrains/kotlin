import java.util.stream.DoubleStream

fun main(args: Array<String>) {
  <caret>  DoubleStream.of(2.3, 3.4, 5.6).boxed().count()
}