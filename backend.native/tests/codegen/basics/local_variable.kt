package codegen.basics.local_variable

import kotlin.test.*

fun local_variable(a: Int) : Int {
  var b = 0
  b = a + 11
  return b
}

@Test
fun runTest() {
  if (local_variable(3) != 14) throw Error()
}