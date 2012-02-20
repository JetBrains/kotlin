fun box() : String {
  val fps  : Double = 1.double
  var mspf : Long
  {
    if ((fps.int == 0))
      mspf = 0
    else
      mspf = (((1000.0 / fps)).long)
  }
  return "OK"
}