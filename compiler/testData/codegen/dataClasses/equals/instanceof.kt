class Dummy {
  fun equals(other: Any?) = true
}

open data class A(val v: Any)

class B(v: Any) : A(v)

fun box() : String {
  val a = A(Dummy())
  val b = B(Dummy())
  return if(b == a) "OK" else "fail"
}