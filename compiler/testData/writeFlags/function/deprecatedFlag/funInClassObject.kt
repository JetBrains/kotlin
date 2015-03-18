class MyClass() {
    companion object {
        deprecated("") fun test() {}
    }
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: MyClass$Companion, test
// FLAGS: ACC_DEPRECATED, ACC_PUBLIC, ACC_FINAL
