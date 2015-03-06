// NO_FLAGS because we put enum in default object of foo. When it will be fixed - MyClass should have ACC_PRIVATE flag

class Foo {
  private enum class MyClass() {
  }
}

// TESTED_OBJECT_KIND: class
// TESTED_OBJECTS: Foo$MyClass
// FLAGS: ACC_FINAL, ACC_SUPER, ACC_ENUM
