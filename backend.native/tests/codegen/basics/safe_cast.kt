open class A
class B : A()
class C

fun foo(a: Any) : A? = a as? A

fun safe_cast_positive(): Boolean {
  val b = B()
  return foo(b) === b
}

fun safe_cast_negative(): Boolean {
  val c = C()
  return foo(c) == null
}


fun main(args: Array<String>) {
  val safeCastPositive = safe_cast_positive().toString()
  val safeCastNegative = safe_cast_negative().toString()
  println("safe_cast_positive: " + safeCastPositive)
  println("safe_cast_negative: " + safeCastNegative)
}