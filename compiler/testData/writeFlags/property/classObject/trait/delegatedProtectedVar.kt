class TestDelegate() {
    fun getValue(thisRef: Any?, desc: PropertyMetadata): Int {
        return 10
    }

    public open fun setValue(thisRef: Any?, desc: PropertyMetadata, svalue : Int) {

    }
}

interface Test {
  companion object {
    protected var prop: Int by TestDelegate()
  }
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop
// ABSENT: TRUE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$Companion, prop
// ABSENT: TRUE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test$Companion, prop$delegate
// FLAGS: ACC_FINAL, ACC_PRIVATE, ACC_STATIC

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop$delegate
// ABSENT: TRUE