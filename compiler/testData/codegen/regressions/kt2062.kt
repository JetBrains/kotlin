fun box(): String {
  val a = if(true) {
  }
  return if (a.toString() == "()") "OK" else "fail"
}
