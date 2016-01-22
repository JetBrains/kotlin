interface Test {
  companion object {
    protected const val prop: Int = 0
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// ABSENT: TRUE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$Companion, prop
// FLAGS: ACC_PROTECTED, ACC_FINAL, ACC_STATIC