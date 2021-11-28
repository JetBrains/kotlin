fun test() {
  val out : Int? = null
  val x : Nothing? = null
  if (out != x)
    out<!UNSAFE_CALL!>.<!>plus(1)
  if (out == x) return
  out<!UNSAFE_CALL!>.<!>plus(1)
}
