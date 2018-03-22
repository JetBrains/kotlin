class MyClass() {
    fun test(s: String = "") {}
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: MyClass, test$default
// FLAGS: ACC_PUBLIC, ACC_STATIC, ACC_SYNTHETIC
