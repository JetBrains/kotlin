class Foo {
  enum class MyClass() {
  }
}

// TESTED_OBJECT_KIND: class
// TESTED_OBJECTS: Foo$MyClass
// FLAGS: ACC_FINAL, ACC_PUBLIC, ACC_SUPER, ACC_ENUM
