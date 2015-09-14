class MyClass() {
    @HiddenDeclaration fun test() {}
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: MyClass, test
// FLAGS: ACC_SYNTHETIC, ACC_PUBLIC, ACC_FINAL
