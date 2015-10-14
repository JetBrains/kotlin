class MyClass() { }

@Deprecated("") fun MyClass.test() {}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: ExtentionFunKt, test
// FLAGS: ACC_DEPRECATED, ACC_PUBLIC, ACC_FINAL, ACC_STATIC
