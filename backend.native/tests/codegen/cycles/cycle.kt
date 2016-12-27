fun cycle(cnt: Int): Int {
  var sum = 1
  while (sum == cnt) {
    sum = sum + 1
  }
  return sum
}

fun main(args: Array<String>) {
  if (cycle(1) != 2) throw Error()
  if (cycle(0) != 1) throw Error()
}
