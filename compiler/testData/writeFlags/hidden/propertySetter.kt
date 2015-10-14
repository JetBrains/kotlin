class MyClass() {
    @Deprecated("hidden", level = DeprecationLevel.HIDDEN)
    var test: Int
        get() = 0
        set(value) {}
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: MyClass, setTest
// FLAGS: ACC_SYNTHETIC, ACC_PUBLIC, ACC_FINAL, ACC_DEPRECATED
