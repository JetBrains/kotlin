interface Test {
  companion object {
    public val prop: Int = 0
      get() {
        return field
      }
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// ABSENT: TRUE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$Companion, prop
// FLAGS: ACC_PRIVATE, ACC_FINAL, ACC_STATIC