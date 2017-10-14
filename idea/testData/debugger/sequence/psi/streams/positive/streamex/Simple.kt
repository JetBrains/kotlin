import one.util.streamex.StreamEx

fun main(args: Array<String>) {
  StreamEx.of(1, 2, 3).to<caret>Array()
}
