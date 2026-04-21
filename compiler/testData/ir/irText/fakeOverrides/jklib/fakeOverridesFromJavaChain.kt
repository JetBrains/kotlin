// TARGET_BACKEND: JKLIB

// FILE: fakeOverridesFromJavaChain/BaseTypeClass.java
package fakeOverridesFromJavaChain;

public class BaseTypeClass {
  public String foo = "abc";

  public String getFoo() {
    return "get:" + foo;
  }

  public void setFoo(String foo) {
    this.foo = "was_set:" + foo;
  }
}

// FILE: fakeOverridesFromJavaChain/SubTypeClass.java
package fakeOverridesFromJavaChain;

public class SubTypeClass extends BaseTypeClass {
  public String bar = "xyz";

  public String getBar() {
    return "get:" + bar;
  }

  public void setBar(String bar) {
    this.bar = "was_set:" + bar;
  }
}

// FILE: fakeOverridesFromJavaChain/Main.kt
package fakeOverridesFromJavaChain

fun assertEquals(expected: Any?, actual: Any?) {
    if (expected != actual) throw AssertionError("Expected $expected but got $actual")
}
class KtType : SubTypeClass()

fun main(vararg unused: String) {
  val ktType = KtType()

  assertEquals("ab", ktType.foo)
  assertEquals("get:abc", ktType.getFoo())

  ktType.setFoo("def")

  assertEquals("was_set:def", ktType.foo)
  assertEquals("get:was_set:def", ktType.getFoo())

  assertEquals("xyz", ktType.bar)
  assertEquals("get:xyz", ktType.getBar())

  ktType.setBar("uvw")
  assertEquals("was_set:uvw", ktType.bar)
  assertEquals("get:was_set:uvw", ktType.getBar())
}
