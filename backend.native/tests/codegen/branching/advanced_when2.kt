fun advanced_when2(i: Int): Int {
  var value = 1
  when (i) {
    10 -> {val v = 42; value = v}
    11 -> {val v = 43; value = v}
    12 -> {val v = 44; value = v}
  }

  return value
}

