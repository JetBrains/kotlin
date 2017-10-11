package codegen.function.named

import kotlin.test.*

fun foo(a:Int, b:Int) = a - b

@Test fun runTest() {
  if (foo(b = 24, a = 42) != 18)
      throw Error()
}