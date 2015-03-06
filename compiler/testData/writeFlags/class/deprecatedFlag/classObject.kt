class MyClass {
    deprecated("") default object {

    }
}

// TESTED_OBJECT_KIND: class
// TESTED_OBJECTS: MyClass$Default
// FLAGS: ACC_PUBLIC, ACC_FINAL, ACC_DEPRECATED, ACC_SUPER
