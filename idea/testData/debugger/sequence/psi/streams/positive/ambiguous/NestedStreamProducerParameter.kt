import java.util.stream.Stream

fun main(args: Array<String>) {
<caret>  Stream.of(Stream.of(Stream.of(1).count()).count()).count()
}
