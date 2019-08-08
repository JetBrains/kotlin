class MyClass {
    @Deprecated("") companion object {

    }
}

// TESTED_OBJECT_KIND: class
// TESTED_OBJECTS: MyClass$Companion
// FLAGS: ACC_PUBLIC, ACC_FINAL, ACC_DEPRECATED, ACC_SUPER
