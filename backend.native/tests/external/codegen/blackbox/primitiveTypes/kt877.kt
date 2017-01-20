fun box() : String {
  var a : Int = -1
  if((+a) != -1) return "fail 1"
  a = 1
  if((+a) != 1) return "fail 2"
  if((+-1) != -1) return "fail 3"
  if((-+a) != -1) return "fail 4"
  return "OK"
}
