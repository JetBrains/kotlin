// JVM_DEFAULT_MODE: enable
// WITH_STDLIB
// CHECK_BYTECODE_LISTING
// LANGUAGE: +ImplicitJvmExposeBoxed
// TARGET_BACKEND: JVM_IR

// checking that TestClass has correctly generated mangled and exposed overrides

// FILE: Test.kt
interface Test {
  fun test(p: UInt): UInt = foo(p)
  private val bar: UInt? get() = null
  private fun foo(o: UInt): UInt = o
}

class TestClass : Test {
  override fun test(p: UInt): UInt {
    return super.test(p)
  }
}

// FILE: TestJava.java
public class TestJava {
  public static kotlin.UInt test(kotlin.UInt u) {
    return new TestClass().test(u);
  }
}

// FILE: box.kt
fun box(): String {
  val res = TestJava.test(42u)
  if (res != 42u) return "FAIL $res"
  return "OK"
}