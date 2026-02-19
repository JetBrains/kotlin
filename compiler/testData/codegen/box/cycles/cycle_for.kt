// WITH_STDLIB

import kotlin.test.*

fun cycle_for(arr: Array<Int>) : Int {
  var sum = 0
  for (i in arr) {
    sum += i
  }
  return sum
}

fun box(): String {
  assertEquals(6, cycle_for(Array(4, init = { it })))

  return "OK"
}
