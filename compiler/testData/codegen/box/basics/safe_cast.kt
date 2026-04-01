// WITH_STDLIB
import kotlin.test.*

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

fun box(): String {
  val safeCastPositive = safe_cast_positive().toString()
  val safeCastNegative = safe_cast_negative().toString()
  if (safeCastPositive != "true") return "FAIL safeCastPositive"
  if (safeCastNegative != "true") return "FAIL safeCastNegative"

  return "OK"
}
