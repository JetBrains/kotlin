import java.util.stream.IntStream

fun main(args: Array<String>) {
<caret>  IntStream.of(IntStream.of(1).sum()).sum()
}
