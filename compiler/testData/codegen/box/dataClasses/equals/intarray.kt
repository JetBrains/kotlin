// IGNORE_BACKEND_FIR: JVM_IR
data class A(val v: IntArray)

fun box() : String {
  val myArray = intArrayOf(0, 1, 2)
  if(A(myArray) == A(intArrayOf(0, 1, 2))) return "fail"
  if(A(myArray) != A(myArray)) return "fail 2"
  return "OK"
}