// WITH_STDLIB

import kotlin.test.*

fun foo(x:Int = 0, y:Int = x + 1, z:Int = x + y + 1) = x + y + z

fun box(): String {
  val v = foo()
  if (v != 3) {
    println("test failed $v expected 3")
    throw  Error()
  }
  return "OK"
}
