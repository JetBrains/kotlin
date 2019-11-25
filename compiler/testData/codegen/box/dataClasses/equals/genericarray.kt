// IGNORE_BACKEND_FIR: JVM_IR
data class A(val v: Array<Int>)

fun box() : String {
  val myArray = arrayOf(0, 1, 2)
  if(A(myArray) == A(arrayOf(0, 1, 2))) return "fail"
  if(A(myArray) != A(myArray)) return "fail 2"
  return "OK"
}