class MyClass() {
    public var x: Int by Delegate()
        private set
}

class Delegate {
    fun get(t: Any?, p: PropertyMetadata): Int {
        return 1
    }

    fun set(t: Any?, p: PropertyMetadata, i: Int) {}
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: MyClass, setX
// FLAGS: ACC_FINAL, ACC_PRIVATE
