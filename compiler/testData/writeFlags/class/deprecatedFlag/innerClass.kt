class MyClass() {
    @Deprecated("") public class MyInnerClass() {}
}

// TESTED_OBJECT_KIND: class
// TESTED_OBJECTS: MyClass$MyInnerClass
// FLAGS: ACC_DEPRECATED, ACC_PUBLIC, ACC_FINAL, ACC_SUPER
