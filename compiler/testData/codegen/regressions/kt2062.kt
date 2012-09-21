fun box(): String {
  val a = if(true) {
  }
  return if (a.toString() == "Unit.VALUE") "OK" else "fail"
}
