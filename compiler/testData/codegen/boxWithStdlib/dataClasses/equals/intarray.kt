data class A(val v: IntArray)

fun box() : String {
  if(A(intArrayOf(0,1,2)) != A(intArrayOf(0,1,2))) return "fail"
  return "OK"
}