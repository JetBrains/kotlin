class TestDelegate() {
    fun get(thisRef: Any?, desc: PropertyMetadata): Int {
        return 10
    }

    public open fun set(thisRef: Any?, desc: PropertyMetadata, svalue : Int) {

    }
}

trait Test {
  class object {
    protected var prop: Int by TestDelegate()
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
// FLAGS: ACC_FINAL, ACC_PRIVATE
// ABSENT: TRUE