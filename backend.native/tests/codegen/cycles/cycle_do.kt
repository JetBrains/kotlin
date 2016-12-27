fun cycle_do(cnt: Int): Int {
  var sum = 1
  do {
    sum = sum + 2
  } while (sum == cnt)
  return sum
}

fun main(args: Array<String>) {
  if (cycle_do(3) != 5) throw Error()
  if (cycle_do(0) != 3) throw Error()
}