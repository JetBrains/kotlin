class Test {
  default object {
    public var prop: Int = 0;
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// FLAGS: ACC_STATIC, ACC_PUBLIC

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$Default, prop
// ABSENT: TRUE