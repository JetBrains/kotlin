// WITH_STDLIB

import kotlin.test.*

fun sum(a:Int, b:Int):Int {
 var c:Int
 c = a + b
 return c
}

fun box(): String {
  assertEquals(50, sum(42, 8))
  return "OK"
}
