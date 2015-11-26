import kotlin.reflect.KProperty

class MyClass() {
    public var x: Int by Delegate()
        private set
}

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int {
        return 1
    }

    operator fun setValue(t: Any?, p: KProperty<*>, i: Int) {}
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: MyClass, setX
// FLAGS: ACC_FINAL, ACC_PRIVATE
