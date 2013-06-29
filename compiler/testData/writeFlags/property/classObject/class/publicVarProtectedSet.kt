class Test {
  class object {
    public var prop: Int = 0
      protected set
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// FLAGS: ACC_STATIC, ACC_PROTECTED

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$object, prop
// ABSENT: TRUE