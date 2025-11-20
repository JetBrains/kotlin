// WITH_STDLIB

import kotlin.test.*

class A(val a:Int) {
  fun foo(i:Int) = a + i
}

fun fortyTwo() = A(41).foo(1)

fun box(): String {
  assertEquals(42, fortyTwo())
  return "OK"
}
