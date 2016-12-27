fun if_else(b: Boolean): Int {
  if (b) return 42
  else   return 24
}

fun main(args: Array<String>) {
  if (if_else(false) != 24) throw Error()
}