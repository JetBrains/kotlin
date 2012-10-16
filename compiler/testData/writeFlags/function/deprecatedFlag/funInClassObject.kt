class MyClass() {
    class object {
        Deprecated fun test() {}
    }
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: MyClass$ClassObject$, test
// FLAGS: ACC_DEPRECATED, ACC_PUBLIC, ACC_FINAL
