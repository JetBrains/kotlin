interface Test {
  companion object {
    internal const val prop: Int = 0;
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// FLAGS: ACC_PUBLIC, ACC_FINAL, ACC_STATIC

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$Companion, prop
// FLAGS: ACC_PUBLIC, ACC_FINAL, ACC_STATIC