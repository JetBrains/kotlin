// IGNORE_BACKEND: JS_IR
fun box() : String {
  val fps  : Double = 1.toDouble()
  var mspf : Long
  {
    if ((fps.toInt() == 0))
      mspf = 0
    else
      mspf = (((1000.0 / fps)).toLong())
  }
  return "OK"
}
