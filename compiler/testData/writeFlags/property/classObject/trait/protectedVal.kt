trait Test {
  default object {
    protected val prop: Int = 0
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// ABSENT: TRUE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$Default, prop
// FLAGS: ACC_PRIVATE, ACC_FINAL