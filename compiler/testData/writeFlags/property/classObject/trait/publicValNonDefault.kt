trait Test {
  class object {
    public val prop: Int = 0
      get() {
        return $prop
      }
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// ABSENT: TRUE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$object, prop
// FLAGS: ACC_PRIVATE, ACC_FINAL