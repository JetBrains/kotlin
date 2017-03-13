object A {
  val x: Int = 610
}

fun box() : String {
  return if (A.x != 610) "fail" else "OK"
}
