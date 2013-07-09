class Test {
  class object {
    var prop: Int = 0;
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// FLAGS: ACC_STATIC, ACC_PUBLIC

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$object, prop
// ABSENT: TRUE
