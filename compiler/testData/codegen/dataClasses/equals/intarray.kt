data class A(val v: IntArray)

fun box() : String {
  if(A(intArray(0,1,2)) != A(intArray(0,1,2))) return "fail"
  return "OK"
}