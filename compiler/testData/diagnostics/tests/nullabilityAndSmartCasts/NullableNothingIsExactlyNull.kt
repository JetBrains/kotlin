fun test() {
  val out : Int? = null
  val x : Nothing? = null
  if (out != <!DEBUG_INFO_CONSTANT!>x<!>)
    <!DEBUG_INFO_SMARTCAST!>out<!>.plus(1)
  if (out == <!DEBUG_INFO_CONSTANT!>x<!>) return
  <!DEBUG_INFO_SMARTCAST!>out<!>.plus(1)
}
