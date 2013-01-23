fun box() : String {
  for (i in "".indices) {
      ""[i]
  }
  return "OK"
}

val String?.indices : IntRange get() = 0..(this!!.length - 1)