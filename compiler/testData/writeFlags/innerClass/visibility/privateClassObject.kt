class Foo {
  private companion object {
  }
}

// TESTED_OBJECT_KIND: innerClass
// TESTED_OBJECTS: Foo, Companion
// FLAGS: ACC_FINAL, ACC_PRIVATE, ACC_STATIC
