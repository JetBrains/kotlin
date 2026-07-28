// FILE: ClassObject.java
package test;

class ClassObject {
    void accessToClassObject() {
        WithClassObject.Companion.foo();
        WithClassObject.Companion.getValue();
        WithClassObject.Companion.getValueWithGetter();
        WithClassObject.Companion.getVariable();
        WithClassObject.Companion.setVariable(0);
        WithClassObject.Companion.getVariableWithAccessors();
        WithClassObject.Companion.setVariableWithAccessors(0);
    }

    void accessToPackageObject() {
        PackageInner.INSTANCE.foo();
        PackageInner.INSTANCE.getValue();
    }

    void accessToInnerClass() {
        new WithClassObject.MyInner().foo();
        new WithClassObject.MyInner().getValue();
    }
}

// FILE: ClassObject.kt
package test

class WithClassObject {
  companion object {
    fun foo() {}

    val value: Int = 0
    val valueWithGetter: Int
      get() = 1

    var variable: Int = 0
    var variableWithAccessors: Int
      get() = 0
      set(v) {}

  }

  class MyInner {
    fun foo() {}
    val value: Int = 0
  }
}

object PackageInner {
    fun foo() {}
    val value: Int = 0
}
