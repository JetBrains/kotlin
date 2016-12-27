fun when2(i: Int): Int {
  when (i) {
    0 -> return 42
    else -> return 24
  }
}

fun main(args: Array<String>) {
  if (when2(0) != 42) throw Error()
}