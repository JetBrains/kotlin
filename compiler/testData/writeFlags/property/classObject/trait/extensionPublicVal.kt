trait Test {
  class object {
    public val Test.prop: Int = 0;
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop$ext
// ABSENT: TRUE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$object, prop$ext
// FLAGS: ACC_FINAL, ACC_PRIVATE