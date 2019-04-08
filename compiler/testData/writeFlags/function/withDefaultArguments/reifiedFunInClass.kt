// IGNORE_BACKEND: JVM_IR
class MyClass() {
    inline fun <reified T> test(s: String = "") {}
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: MyClass, test$default
// FLAGS: ACC_STATIC, ACC_SYNTHETIC
