class MyClass() {
    default object {
        deprecated("") fun test() {}
    }
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: MyClass$Default, test
// FLAGS: ACC_DEPRECATED, ACC_PUBLIC, ACC_FINAL
