package codegen.cycles.cycle_for

import kotlin.test.*

fun cycle_for(arr: Array<Int>) : Int {
  var sum = 0
  for (i in arr) {
    sum += i
  }
  return sum
}

@Test fun runTest() {
  if (cycle_for(Array(4, init = { it })) != 6) throw Error()
}