fun local_variable(a: Int) : Int {
  var b = 0
  b = a + 11
  return b
}

fun main(args: Array<String>) {
  if (local_variable(3) != 14) throw Error()
}