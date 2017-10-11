package codegen.`object`.method_call

import kotlin.test.*

class A(val a:Int) {
  fun foo(i:Int) = a + i
}

fun fortyTwo() = A(41).foo(1)

@Test fun runTest() {
  if (fortyTwo() != 42) throw Error()
}