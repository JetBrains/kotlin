class MyClass() {
    var test = 1
        @deprecated("") set(i: Int) { test = i }
}


// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: MyClass, setTest
// FLAGS: ACC_DEPRECATED, ACC_PUBLIC, ACC_FINAL
