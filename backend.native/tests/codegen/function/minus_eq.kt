fun minus_eq(a: Int): Int {
  var b = 11
  b -= a
  return b
}

fun main(args: Array<String>) {
  if (minus_eq(23) != -12) throw Error()
}