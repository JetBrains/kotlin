class Test {
  default object {
    var prop: Int = 0
      private set
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// FLAGS: ACC_STATIC, ACC_PRIVATE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$Default, prop
// ABSENT: TRUE