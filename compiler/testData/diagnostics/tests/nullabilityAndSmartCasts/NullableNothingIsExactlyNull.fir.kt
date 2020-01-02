fun test() {
  val out : Int? = null
  val x : Nothing? = null
  if (out != x)
    out.<!INAPPLICABLE_CANDIDATE!>plus<!>(1)
  if (out == x) return
  out.<!INAPPLICABLE_CANDIDATE!>plus<!>(1)
}
