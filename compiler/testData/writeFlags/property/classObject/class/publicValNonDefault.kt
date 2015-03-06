class Test {
  default object {
    public val prop: Int = 0
      get() {
        return $prop
      }
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// FLAGS: ACC_STATIC, ACC_PRIVATE, ACC_FINAL

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$Default, prop
// ABSENT: TRUE