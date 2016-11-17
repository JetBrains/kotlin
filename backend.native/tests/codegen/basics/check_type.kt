interface I
class A() : I {}
class B() {}

//-----------------------------------------------------------------------------//

fun isTypeOf(a: Any) : Boolean {
  return a is A
}

fun check_type(): Boolean {
  val a = A()
  return isTypeOf(a)
}

//-----------------------------------------------------------------------------//

fun isNotTypeOf(a: Any) : Boolean {
  return a !is A
}

fun check_not_type(): Boolean {
  val b = B()
  return isNotTypeOf(b)
}

//-----------------------------------------------------------------------------//

fun isTypeOfInterface(a: Any) : Boolean {
  return a is I
}

fun check_interface(): Boolean {
  val a = A()
  return isTypeOfInterface(a)
}

//interface AI {
//  fun v():Int
//}
//
//val global:Int = 1
//class A1() : AI {
//  override fun v():Int = global
//}
//
//fun smartCast(a:Any): Int {
//  if (a is AI) {
//    return a.v()
//  }
//  return 24
//}