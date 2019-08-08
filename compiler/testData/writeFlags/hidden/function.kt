class MyClass() {
    @Deprecated("hidden", level = DeprecationLevel.HIDDEN)
    fun test() {}
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: MyClass, test
// FLAGS: ACC_SYNTHETIC, ACC_PUBLIC, ACC_FINAL, ACC_DEPRECATED
