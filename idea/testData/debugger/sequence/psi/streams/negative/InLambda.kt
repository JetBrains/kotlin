import java.util.stream.Stream

fun main(args: Array<String>) {
<caret>  val a = { Stream.of(1, 2).count() }
}
