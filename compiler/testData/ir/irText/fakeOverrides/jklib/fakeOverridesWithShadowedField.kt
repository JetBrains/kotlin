// TARGET_BACKEND: JKLIB

// FILE: fakeOverridesWithShadowedField/BaseTypeClass.java
package fakeOverridesWithShadowedField;

public class BaseTypeClass {
  public String foo = "abc";

  public String getFoo() {
    return "get:" + foo;
  }

  public void setFoo(String foo) {
    this.foo = "was_set:" + foo;
  }
}

// FILE: fakeOverridesWithShadowedField/JavaTypeShadowsField.java
package fakeOverridesWithShadowedField;

public class JavaTypeShadowsField extends BaseTypeClass {
  public String foo = "ijk";
}

// FILE: fakeOverridesWithShadowedField/Main.kt
package fakeOverridesWithShadowedField

fun assertEquals(expected: Any?, actual: Any?) {
    if (expected != actual) throw AssertionError("Expected $expected but got $actual")
}

class KtTypeShadowedFieldInParent : JavaTypeShadowsField()

fun main(vararg unused: String) {
  val shadowedFieldInParent = KtTypeShadowedFieldInParent()

  assertEquals("ijk", shadowedFieldInParent.foo)
  assertEquals("get:abc", shadowedFieldInParent.getFoo())

  shadowedFieldInParent.setFoo("def")
  assertEquals("ijk", shadowedFieldInParent.foo)
  assertEquals("get:was_set:def", shadowedFieldInParent.getFoo())
}
