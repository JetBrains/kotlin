// IGNORE_BACKEND: JVM_IR
class MyClass {
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    @kotlin.internal.InlineOnly
    inline fun test(s: String) {}
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: MyClass, test
// FLAGS: ACC_PRIVATE, ACC_FINAL
