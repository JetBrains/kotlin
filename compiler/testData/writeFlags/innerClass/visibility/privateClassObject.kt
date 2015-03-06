class Foo {
  private default object {
  }
}

// TESTED_OBJECT_KIND: innerClass
// TESTED_OBJECTS: Foo, Default
// FLAGS: ACC_FINAL, ACC_PRIVATE, ACC_STATIC
