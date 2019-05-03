// IGNORE_BACKEND: JVM_IR
interface Test {
  companion object {
    public var prop: Int = 0
      set(i : Int) {
        field = i
      }
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// ABSENT: TRUE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$Companion, prop
// FLAGS: ACC_PRIVATE, ACC_STATIC