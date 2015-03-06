class TestDelegate() {
    fun get(thisRef: Any?, desc: PropertyMetadata): Int {
        return 10
    }

    public open fun set(thisRef: Any?, desc: PropertyMetadata, svalue : Int) {

    }
}


class Test {
  default object {
    protected var prop: Int by TestDelegate()
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
