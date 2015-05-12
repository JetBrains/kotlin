interface Test {
  companion object {
    var prop: Int
      get() {
        return 10
      }
      set(i : Int) {

      }
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// ABSENT: TRUE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$Companion, prop
// ABSENT: TRUE
