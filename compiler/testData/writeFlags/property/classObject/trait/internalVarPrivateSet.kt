trait Test {
  class object {
    var prop: Int = 0
      private set
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// ABSENT: TRUE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$object, prop
// FLAGS: ACC_PRIVATE