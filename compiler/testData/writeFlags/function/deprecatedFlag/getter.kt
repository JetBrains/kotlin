class MyClass() {
    val test: Int
        @Deprecated("") get(): Int { return 0 }
}


// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: MyClass, getTest
// FLAGS: ACC_DEPRECATED, ACC_PUBLIC, ACC_FINAL
