// TARGET_BACKEND: JKLIB

// FILE: ktjstype/BaseJsTypeClass.java
package ktjstype;

public class BaseJsTypeClass {
  public String foo = "abc";

  public String getFoo() {
    return "get:" + foo;
  }

  public void setFoo(String foo) {
    this.foo = "was_set:" + foo;
  }
}

// FILE: ktjstype/SubJsTypeClass.java
package ktjstype;

public class SubJsTypeClass extends BaseJsTypeClass {
  public String bar = "xyz";

  public String getBar() {
    return "get:" + bar;
  }

  public void setBar(String bar) {
    this.bar = "was_set:" + bar;
  }
}

// FILE: ktjstype/Main.kt
package ktjstype

fun assertEquals(expected: Any?, actual: Any?) {
    if (expected != actual) throw AssertionError("Expected $expected but got $actual")
}
class KtJsType : SubJsTypeClass()

fun main(vararg unused: String) {
  val ktJsType = KtJsType()

  assertEquals("ab", ktJsType.foo)
  assertEquals("get:abc", ktJsType.getFoo())

  ktJsType.setFoo("def")

  assertEquals("was_set:def", ktJsType.foo)
  assertEquals("get:was_set:def", ktJsType.getFoo())

  assertEquals("xyz", ktJsType.bar)
  assertEquals("get:xyz", ktJsType.getBar())

  ktJsType.setBar("uvw")
  assertEquals("was_set:uvw", ktJsType.bar)
  assertEquals("get:was_set:uvw", ktJsType.getBar())
}
