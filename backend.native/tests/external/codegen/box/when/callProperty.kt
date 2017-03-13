class C(val p: Boolean) { }

fun box(): String {
  val c = C(true)

  // Commented for KT-621
  // return when(c) {
  //  .p => "OK"
  //  else => "fail"
  // }
  return if (c.p) "OK" else "fail"
}
