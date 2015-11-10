fun test() {
  val out : Int? = null
  val x : Nothing? = null
  if (out != <!ALWAYS_NULL!>x<!>)
    <!DEBUG_INFO_SMARTCAST!>out<!>.plus(1)
  if (out == <!ALWAYS_NULL!>x<!>) return
  <!DEBUG_INFO_SMARTCAST!>out<!>.plus(1)
}
