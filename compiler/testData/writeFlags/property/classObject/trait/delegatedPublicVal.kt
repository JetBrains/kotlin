class TestDelegate() {
    fun get(thisRef: Any?, desc: PropertyMetadata): Int {
        return 10
    }
}

trait Test {
  class object {
    public val prop: Int by TestDelegate()
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// ABSENT: TRUE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$object, prop
// ABSENT: TRUE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$object, prop$delegate
// FLAGS: ACC_FINAL, ACC_PRIVATE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop$delegate
// ABSENT: TRUE