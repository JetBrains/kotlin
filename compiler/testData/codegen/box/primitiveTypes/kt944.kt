fun box() : String {
  for (i in "".indices) {
      ""[i]
  }
  return "OK"
}

val String?.indices : IntRange get() = IntRange(0, this!!.length)
