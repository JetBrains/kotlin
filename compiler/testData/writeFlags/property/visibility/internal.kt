class Foo {
  inner class MyClass() {
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Foo$MyClass, this$0
// FLAGS: ACC_FINAL, ACC_SYNTHETIC
