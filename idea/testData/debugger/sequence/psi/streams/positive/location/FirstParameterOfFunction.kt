import java.util.stream.IntStream

fun foo(bar: Int) {}

fun main(args: Array<String>) {
<caret>  foo(IntStream.of(1, 2, 3).sum())
}
