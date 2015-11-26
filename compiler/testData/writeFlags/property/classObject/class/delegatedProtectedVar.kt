import kotlin.reflect.KProperty

class TestDelegate() {
    operator fun getValue(thisRef: Any?, desc: KProperty<*>): Int {
        return 10
    }

    operator open fun setValue(thisRef: Any?, desc: KProperty<*>, svalue : Int) {

    }
}


class Test {
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
// ABSENT: TRUE

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: Test, prop$delegate
// FLAGS: ACC_STATIC, ACC_FINAL, ACC_PRIVATE
