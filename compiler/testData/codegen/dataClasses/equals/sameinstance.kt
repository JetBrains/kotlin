data class A()

fun box() : String {
  val a = A()
  val b = a
  return if(b == a) "OK" else "fail"
}