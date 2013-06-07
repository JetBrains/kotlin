class Test {
  class object {
    public val Test.prop: Int = 0;
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop$ext
// FLAGS: ACC_FINAL, ACC_STATIC, ACC_PRIVATE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$object, prop$ext
// ABSENT: TRUE