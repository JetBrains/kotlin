// WITH_STDLIB
import kotlin.test.*

open class A() {}
class B(): A() {}

fun castSimple(o: Any) : A = o as A

fun castTest(): Boolean {
  val b = B()
  castSimple(b)
  return true
}

fun box(): String {
  if (!castTest()) throw Error()

  return "OK"
}
