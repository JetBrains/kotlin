data class A(val v: Array<Int>)

fun box() : String {
  if(A(array(0,1,2)) != A(array(0,1,2))) return "fail"
  return "OK"
}