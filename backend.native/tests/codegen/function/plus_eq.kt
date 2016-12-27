fun plus_eq(a: Int): Int {
  var b = 11
  b += a
  return b
}

fun main(args: Array<String>) {
  if (plus_eq(3) != 14) throw Error()
}