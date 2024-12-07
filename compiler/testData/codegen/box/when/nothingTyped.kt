fun nothingTypedWhenSimple(c: Int): Int {
  val nothing = when(c) {
    2 -> return 2
    3 -> return 5
    4 -> return 1
    5 -> return 6
    6 -> return 9
    else -> return 7746353
  }
}

fun nothingTypedWhenMixed(c: Int): Int {
  val nothing = when(c) {
    2 -> return 2
    3 -> return 5
    23 -> return 211
    44 -> return 666
    11 -> return 43
    43 -> return 53
    55 -> return 2
    99 -> return 81
    3 -> return 21
    212 -> return 5
    66 -> return 1
    5 -> return 611
    6 -> return 12
    123 -> return 63
    1 -> return 11
    5 -> return 19
    6 -> return 9
    else -> return 7746353
  }
}

fun box(): String {
  if (nothingTypedWhenSimple(4) != 1) return "fail1"
  if (nothingTypedWhenMixed(99) != 81) return "fail2"
  return "OK"
}