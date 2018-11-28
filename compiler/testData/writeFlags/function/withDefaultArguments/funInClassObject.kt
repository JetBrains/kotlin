class MyClass() {
    companion object {
        fun test(s: String, x:Int = 10) {}
    }
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: MyClass$Companion, test$default
// FLAGS: ACC_PUBLIC, ACC_STATIC, ACC_SYNTHETIC
