data class A(val v: Array<Int>)

fun box() : String {
  if(A(arrayOf(0,1,2)) != A(arrayOf(0,1,2))) return "fail"
  return "OK"
}