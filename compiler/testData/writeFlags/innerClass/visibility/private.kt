class Foo {
  private inner class MyClass() {
  }
}

// TESTED_OBJECT_KIND: innerClass
// TESTED_OBJECTS: Foo, MyClass
// FLAGS: ACC_FINAL, ACC_PRIVATE, ACC_SUPER
