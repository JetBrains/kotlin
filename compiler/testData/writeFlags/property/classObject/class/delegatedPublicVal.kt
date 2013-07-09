class TestDelegate() {
    fun get(thisRef: Any?, desc: PropertyMetadata): Int {
        return 10
    }
}

class Test {
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
// ABSENT: TRUE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop$delegate
// FLAGS: ACC_STATIC, ACC_FINAL, ACC_PRIVATE
