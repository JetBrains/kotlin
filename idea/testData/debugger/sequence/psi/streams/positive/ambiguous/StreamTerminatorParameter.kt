import java.util.stream.IntStream

fun main(args: Array<String>) {
<caret> IntStream.of(1, 2).reduce(IntStream.of(1, 2, 3).sum()) { l, r -> l + r }
}
