// TARGET_BACKEND: JKLIB

// FILE: ktjstypeshadowedfield/BaseJsTypeClass.java
package ktjstypeshadowedfield;

public class BaseJsTypeClass {
  public String foo = "abc";

  public String getFoo() {
    return "get:" + foo;
  }

  public void setFoo(String foo) {
    this.foo = "was_set:" + foo;
  }
}

// FILE: ktjstypeshadowedfield/JavaJsTypeShadowsField.java
package ktjstypeshadowedfield;

public class JavaJsTypeShadowsField extends BaseJsTypeClass {
  public String foo = "ijk";
}

// FILE: ktjstypeshadowedfield/Main.kt
package ktjstypeshadowedfield

fun assertEquals(expected: Any?, actual: Any?) {
    if (expected != actual) throw AssertionError("Expected $expected but got $actual")
}

class KtJsTypeShadowedFieldInParent : JavaJsTypeShadowsField()

fun main(vararg unused: String) {
  val shadowedFieldInParent = KtJsTypeShadowedFieldInParent()

  assertEquals("ijk", shadowedFieldInParent.foo)
  assertEquals("get:abc", shadowedFieldInParent.getFoo())

  shadowedFieldInParent.setFoo("def")
  assertEquals("ijk", shadowedFieldInParent.foo)
  assertEquals("get:was_set:def", shadowedFieldInParent.getFoo())
}
