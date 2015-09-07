class MyClass() {
    @HiddenDeclaration var test: Int
        get() = 0
        set(value) {}
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: MyClass, getTest
// FLAGS: ACC_SYNTHETIC, ACC_PUBLIC, ACC_FINAL
