class TestDelegate() {
    fun get(thisRef: Any?, desc: PropertyMetadata): Int {
        return 10
    }
}

class Test {
  default object {
    public val prop: Int by TestDelegate()
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// ABSENT: TRUE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$Default, prop
// ABSENT: TRUE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$Default, prop$delegate
// ABSENT: TRUE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop$delegate
// FLAGS: ACC_STATIC, ACC_FINAL, ACC_PRIVATE
