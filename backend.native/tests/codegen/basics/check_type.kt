interface I
class A() : I {}
class B() {}

//-----------------------------------------------------------------------------//

fun isTypeOf(a: Any?) : Boolean {
  return a is A
}

//-----------------------------------------------------------------------------//

fun isTypeNullableOf(a: Any?) : Boolean {
  return a is A?
}

//-----------------------------------------------------------------------------//

fun isNotTypeOf(a: Any) : Boolean {
  return a !is A
}

//-----------------------------------------------------------------------------//

fun isTypeOfInterface(a: Any) : Boolean {
  return a is I
}

//-----------------------------------------------------------------------------//

fun main(args: Array<String>) {

  println(isTypeOf(A()))
  println(isTypeOf(null))
  println(isTypeNullableOf(A()))
  println(isTypeNullableOf(null))
  println(isNotTypeOf(B()))
  println(isTypeOfInterface(A()))
}
