fun box(): String {
  val a = if(true) {
  }
  return if (a.toString() == "kotlin.Unit") "OK" else "fail"
}
