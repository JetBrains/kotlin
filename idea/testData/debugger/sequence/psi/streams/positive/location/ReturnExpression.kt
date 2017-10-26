import java.util.stream.IntStream

fun foo(): Int {
<caret>  return IntStream.range(1, 2).sum()
}

fun main(args: Array<String>) {
  foo()
}
