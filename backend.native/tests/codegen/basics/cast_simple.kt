package codegen.basics.cast_simple

import kotlin.test.*

open class A() {}
class B(): A() {}

fun castSimple(o: Any) : A = o as A

fun castTest(): Boolean {
  val b = B()
  castSimple(b)
  return true
}

@Test
fun runTest() {
  if (!castTest()) throw Error()
}