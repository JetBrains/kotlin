import java.util.function.IntPredicate

private class IntStream {

  fun filter(predicate: IntPredicate): IntStream {
    return this
  }

  fun sum(): Int {
    return 0
  }

  companion object {
    fun of(vararg a: Int): IntStream {
      return IntStream()
    }
  }
}

fun main(args: Array<String>) {
<caret>  val s = IntStream.of(1, 2).filter(IntPredicate { it % 2 == 0 }).sum()
}
