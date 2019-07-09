// KJS_WITH_FULL_RUNTIME
var x = 0
fun inc(): Int {
    x++
    return 0
}
fun box(): String {
  val al = ArrayList<Int>()
  when (inc()) {
      in al -> return "fail 1"
      else -> {}
  }
  return if (x == 1) "OK" else "fail 2"
}
