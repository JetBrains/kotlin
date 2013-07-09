class Test {
  class object {
    public var prop: Int = 0
      set(i : Int) {
        $prop = i
      }
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// FLAGS: ACC_STATIC, ACC_PRIVATE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$object, prop
// ABSENT: TRUE