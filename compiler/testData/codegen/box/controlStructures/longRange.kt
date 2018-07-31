fun box(): String {
  val r = 1.toLong()..2
  var s = ""
  for (l in r) {
    s += l
  }
  return if (s == "12") "OK" else "fail: $s"
}
