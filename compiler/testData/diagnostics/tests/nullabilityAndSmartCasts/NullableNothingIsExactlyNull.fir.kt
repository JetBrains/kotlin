fun test() {
  val out : Int? = null
  val x : Nothing? = null
  if (out != x)
    out.<!NONE_APPLICABLE!>plus<!>(1)
  if (out == x) return
  out.<!NONE_APPLICABLE!>plus<!>(1)
}
