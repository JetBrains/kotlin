trait Test {
  class object {
    val prop: Int = 0;
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// FLAGS: ACC_STATIC, ACC_PUBLIC, ACC_FINAL

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$object, prop
// FLAGS: ACC_PRIVATE, ACC_FINAL