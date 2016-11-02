fun cycle_do(cnt: Int): Int {
  var sum = 1
  do {
    sum = sum + 2
  } while (sum == cnt)
  return sum
}
